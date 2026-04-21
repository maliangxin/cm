package com.yonyoucloud.fi.cmp.reconciliate;

import cn.hutool.core.thread.BlockPolicy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.filter.util.StringUtil;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.basecom.util.HttpTookitYts;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.basecom.utils.HttpTookit;
import com.yonyoucloud.fi.cmp.balanceadjustresult.BalanceAdjustResult;
import com.yonyoucloud.fi.cmp.bankautocheckconfig.BankAutoCheckConfig;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankAutoCheckConfigService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankBillSmartCheckService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting;
import com.yonyoucloud.fi.cmp.cmpcheck.service.CmpCheckService;
import com.yonyoucloud.fi.cmp.cmpentity.EnableStatus;
import com.yonyoucloud.fi.cmp.cmpentity.ReconciliationBasisType;
import com.yonyoucloud.fi.cmp.cmpentity.ReconciliationDataSource;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.ctmrpc.CtmCmpBankReconciliationSettingRpcServiceImpl;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.BankReconciliationSettingVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.PlanParam;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.receivebill.service.ReceiveBillServiceImpl;
import com.yonyoucloud.fi.cmp.reconciliate.vo.ReconciliationInfoVO;
import com.yonyoucloud.fi.cmp.reconciliation.ReconciliationMatchRecord;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolUtil;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import com.yonyoucloud.fi.egl.voucher.api.v1.IVoucherBankRpcService;
import com.yonyoucloud.fi.egl.voucher.dto.cash.CashVoucherCheckInfoDTO;
import com.yonyoucloud.fi.egl.voucher.dto.cash.CheckInfoDTO;
import com.yonyoucloud.fi.egl.voucher.dto.cash.ResultDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.imeta.biz.base.Objectlizer;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.base.Json;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * @description: 银企对账后台操作具体实现
 * 自动对账，手工对账，单边对账，取消对账等具体实现
 *
 */

@Service
@Transactional(rollbackFor = RuntimeException.class)
@Slf4j
public class ReconciliateServiceImpl implements ReconciliateService {
    //线程池
    static ExecutorService executorService = null;
    static {
        executorService = ThreadPoolBuilder.ioThreadPoolBuilder()
                .setQueueSize(100)
                .setDaemon(false)
                .setMaximumPoolSize(100)
                .setRejectHandler(new BlockPolicy())
                .builder(50, 128,200,"cmp-reconciliation-cancelTick-async-");
    }
    private static final String DETAILPREX = "com.yonyoucloud.fi.cmp.mapper.UpdateCheckFlagMapper";
    private static final String JOURNALS = "journals";
    private static final String BANKRECONCILIATIONS = "bankreconciliations";
    private static final String RECONCILIATIONDATASOURCEID = "reconciliationdatasourceid";
    private static final String RECONCILIATIONDATASOURCE = "reconciliationdatasource";
    private static final String ENABLEDATE = "enableDate";
    private static final String BANKRECONCILIATIONSETTING_B = "bankReconciliationSetting_b";
    @Autowired
    CmpVoucherService cmpVoucherService;
    @Autowired
    ReceiveBillServiceImpl receiveBillServiceImpl;
    @Autowired
    private JournalService journalService;
    @Resource
    private BankAutoCheckConfigService bankAutoCheckConfigService;
    @Autowired
    CtmCmpBankReconciliationSettingRpcServiceImpl cmpBankReconciliationSettingRpcService;
    @Autowired
    private BankBillSmartCheckService bankBillSmartCheckService;
    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;
    @Autowired
    private CmpCheckService cmpCheckService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    //自动勾对,一对一勾对
    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public CtmJSONObject automateTick(CtmJSONObject jsonObject) throws  Exception{
        CtmJSONObject reback = new CtmJSONObject();
        String key  = jsonObject.getString(RECONCILIATIONDATASOURCEID);
        Integer reconciliationdatasource = jsonObject.getInteger(RECONCILIATIONDATASOURCE);
        Json json = new Json(CtmJSONObject.toJSONString(jsonObject.getJSONArray(JOURNALS)));
        List<Journal> journalList = Objectlizer.decode(json, Journal.ENTITY_NAME);
        json = new Json(CtmJSONObject.toJSONString(jsonObject.getJSONArray(BANKRECONCILIATIONS)));
        List<BankReconciliation> bankReconciliationList = Objectlizer.decode(json, BankReconciliation.ENTITY_NAME);
        if((journalList == null || journalList.size() == 0)&&(bankReconciliationList == null || bankReconciliationList.size() == 0)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101298"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800B7","请选择数据") /* "请选择数据" */);
        }
        if (journalList == null || journalList.size() == 0){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101299"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800B8","银行日记账未勾选数据") /* "银行日记账未勾选数据" */);
        }
        if (bankReconciliationList == null || bankReconciliationList.size() == 0){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101300"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800BA","银行对账单未勾选数据") /* "银行对账单未勾选数据" */);
        }
        Integer  journalSize = journalList.size();
        Integer  bankReconciliationSize = bankReconciliationList.size();
        List<Journal> journals = new ArrayList<>();
        List<BankReconciliation> banks = new ArrayList<>();

        //对账方案明细停用，后台不支持勾对；明细可能存在同样的数据但启停用状态不一致，存在停用数据但是也有启用数据则跳过该校验
        String bankreconciliationscheme = jsonObject.getString(RECONCILIATIONDATASOURCEID);
        //校验银行账号
        checkBankAccounts(bankreconciliationscheme,journalList,bankReconciliationList);

        //自动对账优化，查询对应自动对账设置
        BankAutoCheckConfig bankAutoCheckConfig = bankAutoCheckConfigService.queryConfigInfo(null);

        //检查组织锁是否存在
        YmsLock ymsLock = null;
        try {
            //加锁
            ymsLock = JedisLockUtils.lockDzWithOutTrace(key);
            if (ymsLock == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101302"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800CC","该组织下正在银行对账，请稍后再试！") /* "该组织下正在银行对账，请稍后再试！" */);
            }
            //凭证勾兑回单文件需求
            //key：勾兑号 value:勾兑的对账单集合
            Map<String,List<BankReconciliation>> bankReceiptMap = new HashMap<>();
            //key：勾兑号 value:勾兑的凭证集合
            Map<String,List<Journal>> journalReceiptMap = new HashMap<>();


            //账户共享，左侧日记账授权使用组织集合
            Set<String> journalAccentitySet = new HashSet<>();
            //日记账和对账单全部授权使用组织合集
            Set<String> allAccentitySet = new HashSet<>();
            Iterator<Journal> journalIterator = journalList.iterator();
            while(journalIterator.hasNext()){
                Journal journal = journalIterator.next();
                if(journal.getCheckflag() != null){
                    if(journal.getCheckflag()){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101303"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A1","已经勾对的不能再次勾对!") /* "已经勾对的不能再次勾对!" */);
                    }
                }
                if(!StringUtils.isEmpty(journal.getAccentity())){
                    journalAccentitySet.add(journal.getAccentity());
                    allAccentitySet.add(journal.getAccentity());
                }
            }
            Iterator<BankReconciliation> bankIterator = bankReconciliationList.iterator();
            while (bankIterator.hasNext()){
                BankReconciliation bank = bankIterator.next();
                if(ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
                    if(bank.getOther_checkflag()){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101303"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A1","已经勾对的不能再次勾对!") /* "已经勾对的不能再次勾对!" */);
                    }
                }else{
                    if(bank.getCheckflag()){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101303"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A1","已经勾对的不能再次勾对!") /* "已经勾对的不能再次勾对!" */);
                    }
                }
                if(!StringUtils.isEmpty(bank.getAccentity())){
                    allAccentitySet.add(bank.getAccentity());
                }
            }

            //20260130 增加勾对关系记录
            List<ReconciliationInfoVO> reconciliationInfoVOList = new ArrayList<>();
            //step1: 202408 优先根据财资统一对账码对账(sourceType=1)
            List<ReconciliationInfoVO> step1List = bankBillSmartCheckService.handleBySmartCheckNo(1,journals,banks,bankReconciliationList,journalList,Long.valueOf(bankreconciliationscheme),reconciliationdatasource);
            reconciliationInfoVOList.addAll(step1List);
            //step2: 202405 再根据银行对账编码进行对账(sourceType=2)
            List<ReconciliationInfoVO> step2List = bankBillSmartCheckService.handleBySmartCheckNo(2,journals,banks,bankReconciliationList,journalList,Long.valueOf(bankreconciliationscheme),reconciliationdatasource);
            reconciliationInfoVOList.addAll(step2List);
            //step3:20260115 判断是否根据关键要素对账；自动对账设置为空时，默认进行关键要素对账
            if (bankAutoCheckConfig == null || bankAutoCheckConfig.getKeyElementMatchFlag() == (short)1){
                List<ReconciliationInfoVO> step3List = bankBillSmartCheckService.handleByAutomaticRulesCheckFactor(journals,banks,bankReconciliationList,journalList,Long.valueOf(bankreconciliationscheme),reconciliationdatasource,bankAutoCheckConfig);
                reconciliationInfoVOList.addAll(step3List);
            }

            //处理勾对
            bankBillSmartCheckService.handleJournalAndBankCheck(journals, banks,reconciliationdatasource);
            //20260130 记录勾对关系；新增勾对关系记录表数据
            bankBillSmartCheckService.saveReconciliationMathRecord(reconciliationInfoVOList);

            //凭证关联回单:数据组装
            if(ReconciliationDataSource.Voucher.getValue() == reconciliationdatasource){
                for (BankReconciliation bankToVoucher :banks){
                    if (bankReceiptMap.containsKey(bankToVoucher.getOther_checkno())){
                        bankReceiptMap.get(bankToVoucher.getOther_checkno()).add(bankToVoucher);
                    }else {
                        List<BankReconciliation> bankReconciliations = new ArrayList<>();
                        bankReconciliations.add(bankToVoucher);
                        bankReceiptMap.put(bankToVoucher.getOther_checkno(),bankReconciliations);
                    }
                }
                for (Journal journalToVoucher : journals){
                    if (journalReceiptMap.containsKey(journalToVoucher.getCheckno())){
                        journalReceiptMap.get(journalToVoucher.getCheckno()).add(journalToVoucher);
                    }else {
                        List<Journal> journalsReceiptList = new ArrayList<>();
                        journalsReceiptList.add(journalToVoucher);
                        journalReceiptMap.put(journalToVoucher.getCheckno(),journalsReceiptList);
                    }
                }
            }

            //凭证勾对，发送回单关联消息到总账
            if(ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
                ExecutorService taskExecutor = null;
                try {
                    taskExecutor  = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1,2,200,"handleBankReceiptEvent-threadpool");
                    taskExecutor.submit(() -> {
                        try {
                            //凭证关联回单，事件发送
                            cmpCheckService.handleBankReceiptEvent(bankReceiptMap,journalReceiptMap);
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                    });
                }catch (Exception e){
                    log.error(e.getMessage(), e);
                }finally {
                    if (taskExecutor!=null){
                        taskExecutor.shutdown();
                    }
                }
            }
            reback.put("success", true);
            StringBuffer message = new StringBuffer(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A6","银行日记账已勾对笔数:") /* "银行日记账已勾对笔数:" */ + journals.size() + "\r\n");
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A8","银行日记账未勾对笔数:") /* "银行日记账未勾对笔数:" */ + (journalSize - journals.size()) + "\r\n");
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A9","对账单已勾对笔数：") /* "对账单已勾对笔数：" */ + banks.size() + "\r\n");
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800AB","对账单未勾对笔数:") /* "对账单未勾对笔数:" */ + (bankReconciliationSize - banks.size()) + "\r\n");
            reback.put("message", message);

        } catch (Exception e) {
            log.error("自动对账失败--" + e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101310"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800AD","自动对账失败,") /* "自动对账失败," */ + e.getMessage());
        }finally {
            //释放组织锁
            JedisLockUtils.unlockDzWithOutTrace(ymsLock);
        }
        return reback;
    }

    /**
     * 单边勾对
     */
    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public CtmJSONObject onesideTick(CtmJSONObject jsonObject){
        CtmJSONObject reback = new CtmJSONObject();
        String key = jsonObject.getString(RECONCILIATIONDATASOURCEID);
        YmsLock ymsLock = null;
        try {
            Integer reconciliationdatasource = jsonObject.getInteger(RECONCILIATIONDATASOURCE);
            Json json = new Json(jsonObject.getJSONArray(JOURNALS).toString());
            List<Journal> journalList = Objectlizer.decode(json, Journal.ENTITY_NAME);
            json = new Json(jsonObject.getJSONArray(BANKRECONCILIATIONS).toString());
            List<BankReconciliation> bankReconciliationList = Objectlizer.decode(json, BankReconciliation.ENTITY_NAME);
            if ((journalList != null && journalList.size() > 0) && (bankReconciliationList != null && bankReconciliationList.size() > 0)){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101311"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800B3","单边勾对只允许勾对单边数据") /* "单边勾对只允许勾对单边数据" */);
            }
            if ((journalList == null || journalList.size() == 0) && (bankReconciliationList == null || bankReconciliationList.size() == 0)){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101312"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800B5","未勾选任何数据") /* "未勾选任何数据" */);
            }
            //勾对时间
            Date date = BillInfoUtils.getBusinessDate();//业务日期
            Date checkdate = date == null? new Date() : date;
            String checkno = "";
            //勾对人
            Long checkman = queryOperator();
            //对账方案明细停用，后台不支持勾对；明细可能存在同样的数据但启停用状态不一致，存在停用数据但是也有启用数据则跳过该校验
            String bankreconciliationscheme = jsonObject.getString(RECONCILIATIONDATASOURCEID);
            //校验银行账号
            checkBankAccounts(bankreconciliationscheme,journalList,bankReconciliationList);

            //账户共享，左侧日记账授权使用组织集合
            Set<String> journalAccentitySet = new HashSet<>();
            //全部账户使用组织
            Set<String> allAccentitySet = new HashSet<>();
            if (journalList != null && journalList.size() > 0){
                for (Journal j : journalList){
                    if (!StringUtils.isEmpty(j.getAccentity())){
                        journalAccentitySet.add(j.getAccentity());
                        allAccentitySet.add(j.getAccentity());
                    }else {
                        journalAccentitySet.add("");
                        allAccentitySet.add("");
                    }
                }
            }
            if (bankReconciliationList != null && bankReconciliationList.size() > 0){
                for (BankReconciliation b : bankReconciliationList){
                    if (!StringUtils.isEmpty(b.getAccentity())){
                        allAccentitySet.add(b.getAccentity());
                    }else {
                        allAccentitySet.add("");
                    }
                }
            }
            //账户共享校验左侧勾选数据中，授权使用组织是否一致，如不一致，提示：”勾选数据中存在不同的授权使用组织，不允许同时勾对，请检查！“
            //校验左侧与右侧勾选数据中，授权使用组织是否一致(左侧有值，右侧为空时默认一致)，如不一致，提示：”勾选数据中存在不同的授权使用组织，不允许同时勾对，请检查！“
            if(ReconciliationDataSource.BankJournal.getValue() == reconciliationdatasource  && (journalAccentitySet.size() > 1 || allAccentitySet.size() > 1)){
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F3C48A20508000A", "勾选数据中存在不同的账户使用组织，不允许同时勾对，请检查！") /* "勾选数据中存在不同的账户使用组织，不允许同时勾对，请检查！" */);
            }

            if (journalList != null && journalList.size() > 0){
                //加锁
                ymsLock = JedisLockUtils.lockDzWithOutTrace(key);
                if (ymsLock == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101302"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800CC","该组织下正在银行对账，请稍后再试！") /* "该组织下正在银行对账，请稍后再试！" */);
                }
                List<Journal> toUpdateJournalList = new ArrayList<Journal>();
                //List<Long> toUpdateIds = new ArrayList<>();
                //按银行账号进行分组
                Map<String,List<Journal>> journalBankaccounts  = groupByJournalList(journalList);
                for(List<Journal> journalBankaccountList : journalBankaccounts.values()){
                    //勾对号;20260130需求调整为 UE + 勾对业务日期 + 19位OID
                    checkno = "UE" + DateUtils.parseDateToStr(checkdate,"yyyyMMdd") + "-" + ymsOidGenerator.nextStrId();
                    List<Journal> toUpdateJournalListTemp =  new ArrayList<Journal>();
                    //List<Long> toUpdateIdsTemp = new ArrayList<>();
                    String currencyFlag = String.valueOf(journalBankaccountList.get(0).getCurrency());
                    BigDecimal amount = BigDecimal.ZERO;
                    for (Journal journal : journalBankaccountList){
                        if(journal.getCheckflag() != null){
                            if(journal.getCheckflag()){
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101303"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A1","已经勾对的不能再次勾对!") /* "已经勾对的不能再次勾对!" */);
                            }
                        }
                        String currency = String.valueOf(journal.getCurrency());
                        if (!currencyFlag.equals(currency)) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101313"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800AF","对账币种不一致") /* "对账币种不一致" */);
                        }
                        Direction direction = journal.getDirection();
                        if (direction != null && direction.equals(Direction.Debit)){
                            amount = BigDecimalUtils.safeAdd(amount, journal.getDebitoriSum());
                        }else {
                            amount = BigDecimalUtils.safeSubtract(false, amount, journal.getCreditoriSum());
                        }
                        Journal journalNew = new Journal();
                        journalNew.setId(journal.getId());
                        journalNew.setSrcbillitemid(journal.getSrcbillitemid());
                        journalNew.setTradetype(journal.getTradetype());
                        journalNew.setDefine1(journal.get("ts"));
                        packBill(null, journalNew, checkno, checkdate, true, checkman, SettleStatus.alreadySettled,reconciliationdatasource,key);
                        toUpdateJournalListTemp.add(journalNew);
                    }
                    if (amount.compareTo(BigDecimal.ZERO) != 0){
                        continue;
                    }else {
                        toUpdateJournalList.addAll(toUpdateJournalListTemp);
                    }
                }
                if (toUpdateJournalList.size() == 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101314"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A2","没有符合勾对的数据") /* "没有符合勾对的数据" */);
                }
                if(ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
                    //调用总账接口更新勾兑状态、勾兑号
                    batchUpdateCheckFlag(toUpdateJournalList);
                }else{
                    MetaDaoHelper.update(Journal.ENTITY_NAME, toUpdateJournalList);
                }
                //组装勾对关系记录表信息
                ReconciliationInfoVO reconciliationInfoVO = bankBillSmartCheckService.handleReconciliationInfoVO(journalList,null,
                        ReconciliationBasisType.SingleSideMatching.getValue(),null,bankreconciliationscheme,reconciliationdatasource,checkno,checkdate);
                //记录勾对关系
                bankBillSmartCheckService.saveReconciliationMathRecord(Collections.singletonList(reconciliationInfoVO));
            }else {//银行对账单数据单边对账
                //加锁
                ymsLock = JedisLockUtils.lockDzWithOutTrace(key);
                if (ymsLock == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101302"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800CC","该组织下正在银行对账，请稍后再试！") /* "该组织下正在银行对账，请稍后再试！" */);
                }
                List<BankReconciliation> toUpdateBankReconciliationList = new ArrayList<BankReconciliation>();
                //按银行账号进行分组
                Map<String,List<BankReconciliation>> bankReconciliationBankaccounts  = groupByBankReconciliationList(bankReconciliationList);
                for(List<BankReconciliation> bankReconciliationBankaccountList : bankReconciliationBankaccounts.values()){
                    //勾对号;20260130需求调整为 UE + 勾对业务日期 + 19位OID
                    checkno = "UE" + DateUtils.parseDateToStr(checkdate,"yyyyMMdd") + "-" + ymsOidGenerator.nextStrId();
                    String currencyFlag = String.valueOf(bankReconciliationBankaccountList.get(0).getCurrency());
                    BigDecimal amount = BigDecimal.ZERO;
                    for (BankReconciliation bank : bankReconciliationBankaccountList) {
                        if(ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
                            if(bank.getOther_checkflag()){
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101303"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A1","已经勾对的不能再次勾对!") /* "已经勾对的不能再次勾对!" */);
                            }
                        } else {
                            if (bank.getCheckflag()) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101303"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A1","已经勾对的不能再次勾对!") /* "已经勾对的不能再次勾对!" */);
                            }
                        }
                        String currency = String.valueOf(bank.getCurrency());
                        if (!currencyFlag.equals(currency)) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101313"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800AF","对账币种不一致") /* "对账币种不一致" */);
                        }
                        Direction direction = bank.getDc_flag();
                        if (direction != null && direction.equals(Direction.Debit)) {
                            amount = BigDecimalUtils.safeAdd(amount, bank.getDebitamount());
                        } else {
                            amount = BigDecimalUtils.safeSubtract(false, amount, bank.getCreditamount());
                        }
                        packBill(bank, null, checkno, checkdate, true, checkman, null,reconciliationdatasource,key);
                    }
                    if (amount.compareTo(BigDecimal.ZERO) != 0){
                        continue;
                    }else {
                        toUpdateBankReconciliationList.addAll(bankReconciliationBankaccountList);
                    }
                }
                if(toUpdateBankReconciliationList.size()==0){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101314"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A2","没有符合勾对的数据") /* "没有符合勾对的数据" */);
                }
                CommonSaveUtils.updateBankReconciliation4Check(toUpdateBankReconciliationList,reconciliationdatasource);
                //组装勾对关系记录表信息
                ReconciliationInfoVO reconciliationInfoVO = bankBillSmartCheckService.handleReconciliationInfoVO(null,toUpdateBankReconciliationList,
                        ReconciliationBasisType.SingleSideMatching.getValue(),null,bankreconciliationscheme,reconciliationdatasource,checkno,checkdate);
                //记录勾对关系
                bankBillSmartCheckService.saveReconciliationMathRecord(Collections.singletonList(reconciliationInfoVO));
            }
            reback.put("success", true);
            reback.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800B2","对账勾对成功") /* "对账勾对成功" */);
            return reback;
        } catch (Exception e) {
            reback.put("success", false);
            reback.put("message", e.getMessage());
            log.error("单边对账失败--"  + e.getMessage(),e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101315"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800B6","单边对账失败,") /* "单边对账失败," */ + e.getMessage());
        }finally {
            JedisLockUtils.unlockDzWithOutTrace(ymsLock);
        }
    }


    /**
     * 手动勾对,可以一对一,也可以一对多,多对多
     */
    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public CtmJSONObject handTick(CtmJSONObject jsonObject) throws Exception {
        CtmJSONObject reback = new CtmJSONObject();
        String key = jsonObject.getString(RECONCILIATIONDATASOURCEID);
        Integer reconciliationdatasource = jsonObject.getInteger(RECONCILIATIONDATASOURCE);
        Json json = new Json(CtmJSONObject.toJSONString(jsonObject.getJSONArray(JOURNALS)));
        List<Journal> journalList = Objectlizer.decode(json, Journal.ENTITY_NAME);
        json = new Json(CtmJSONObject.toJSONString(jsonObject.getJSONArray(BANKRECONCILIATIONS)));
        List<BankReconciliation> bankReconciliationList = Objectlizer.decode(json, BankReconciliation.ENTITY_NAME);
        if ((journalList == null || journalList.size() == 0) && (bankReconciliationList == null || bankReconciliationList.size() == 0))
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101312"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800B5","未勾选任何数据") /* "未勾选任何数据" */);
        if(bankReconciliationList == null || bankReconciliationList.size() == 0){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101316"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800BB","银行对账单不能为空") /* "银行对账单不能为空" */);
        }
        if(journalList == null || journalList.size() == 0){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101317"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800BD","企业日记账不能为空") /* "企业日记账不能为空" */);
        }
        //对账方案明细停用，后台不支持勾对；明细可能存在同样的数据但启停用状态不一致，存在停用数据但是也有启用数据则跳过该校验
        String bankreconciliationscheme = jsonObject.getString(RECONCILIATIONDATASOURCEID);
        //校验银行账号
        checkBankAccounts(bankreconciliationscheme,journalList,bankReconciliationList);

        //账户共享，左侧日记账授权使用组织集合
        Set<String> journalAccentitySet = new HashSet<>();
        //对账单全部授权使用组织合集
        Set<String> allAccentitySet = new HashSet<>();
        for (Journal j : journalList){
            if (!StringUtils.isEmpty(j.getAccentity())){
                journalAccentitySet.add(j.getAccentity());
                allAccentitySet.add(j.getAccentity());
            }else {
                journalAccentitySet.add("");
                allAccentitySet.add("");
            }
        }
        for (BankReconciliation b : bankReconciliationList){
            if (!StringUtils.isEmpty(b.getAccentity())){
                allAccentitySet.add(b.getAccentity());
            }else {
                allAccentitySet.add("");
            }
        }
        //账户共享校验左侧勾选数据中，授权使用组织是否一致，如不一致，提示：”勾选数据中存在不同的授权使用组织，不允许同时勾对，请检查！“
        //校验左侧与右侧勾选数据中，授权使用组织是否一致(左侧有值，右侧为空时默认一致)，如不一致，提示：”勾选数据中存在不同的授权使用组织，不允许同时勾对，请检查！“
        if(ReconciliationDataSource.BankJournal.getValue() == reconciliationdatasource  && (journalAccentitySet.size() > 1 || allAccentitySet.size() > 1)){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F3C48A20508000A", "勾选数据中存在不同的账户使用组织，不允许同时勾对，请检查！") /* "勾选数据中存在不同的账户使用组织，不允许同时勾对，请检查！" */);
        }

        YmsLock ymsLock = null;
        try {
            //加锁
            ymsLock = JedisLockUtils.lockDzWithOutTrace(key);
            if (ymsLock == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101302"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800CC","该组织下正在银行对账，请稍后再试！") /* "该组织下正在银行对账，请稍后再试！" */);
            }
            //按银行账号进行分组
            Map<String,List<Journal>> journalBankaccounts  = groupByJournalList(journalList);
            Map<String,List<BankReconciliation>> bankReconciliationBankaccounts  = groupByBankReconciliationList(bankReconciliationList);
            Long checkman = queryOperator();
            //勾对时间
            Date date = BillInfoUtils.getBusinessDate();//业务日期
            Date checkdate = date == null? new Date() : date;
            //勾对号;20260130需求调整为 MR + 勾对业务日期 + 19位OID
            String checkno = "MR" + DateUtils.parseDateToStr(checkdate,"yyyyMMdd") + "-" + ymsOidGenerator.nextStrId();
            List<Journal> toUpdateJournalList = new ArrayList<Journal>();
            List<BankReconciliation> toUpdateBankReconciliationList = new ArrayList<BankReconciliation>();

            //凭证勾兑回单文件需求
            //key：勾兑号 value:勾兑的对账单集合
            Map<String,List<BankReconciliation>> bankReceiptMap = new HashMap<>();
            //key：勾兑号 value:勾兑的凭证集合
            Map<String,List<Journal>> journalReceiptMap = new HashMap<>();

            //遍历银行日记账、对账单集合
            for(String bankAccount : journalBankaccounts.keySet()){
                List<Journal> journalListTemp =  journalBankaccounts.get(bankAccount);
                List<BankReconciliation> bankReconciliationListTemp = bankReconciliationBankaccounts.get(bankAccount);
                if(bankReconciliationListTemp == null || bankReconciliationListTemp.size() == 0){
                    continue;
                }
                // 核心一的checkflag存在值为null的情况
                if(journalListTemp.get(0).getCheckflag() != null){
                    if(journalListTemp.get(0).getCheckflag()){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101303"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A1","已经勾对的不能再次勾对!") /* "已经勾对的不能再次勾对!" */);
                    }
                }

                //使用借贷方向判断
                Direction direction = journalListTemp.get(0).getDirection();
                String leftCurrency = journalListTemp.get(0).getCurrency();
                Map<String,Object> param = new HashMap<String, Object>();
                param.put("checkno",checkno);
                param.put("checkdate",checkdate);
                param.put("checkman",checkman);
                param.put("direction",direction);
                param.put("leftCurrency",leftCurrency);
                param.put("reconciliationdatasource",reconciliationdatasource);
                param.put("bankreconciliationsettingid",key);
                Map<String,Object> journalResult = journalCheck(journalListTemp, param);
                if((Boolean) journalResult.get("isError")){
                    continue;
                }
                List<Journal> toUpdateJournalListTemp = (List<Journal>)journalResult.get("toUpdateJournalListTemp");
                //List<Long> toUpdateIdsTemp = (List<Long>)journalResult.get("toUpdateIdsTemp");
                BigDecimal leftAmount = (BigDecimal)journalResult.get("leftAmount");
                // 负数金额，同方向对账修改
                param.put("leftAmount",leftAmount);
                Map<String,Object> bankReconciliationResult = bankReconciliationCheck(bankReconciliationListTemp,param);
                BigDecimal rightAmount = (BigDecimal)bankReconciliationResult.get("rightAmount");
                if (rightAmount.compareTo(leftAmount) != 0 && leftAmount.compareTo(BigDecimal.ZERO) >= 0) {
                    continue;
                } else if (leftAmount.compareTo(BigDecimal.ZERO) < 0 && leftAmount.abs().compareTo(rightAmount) != 0){
                    continue;
                } else {
                    toUpdateJournalList.addAll(toUpdateJournalListTemp);
                    toUpdateBankReconciliationList.addAll(bankReconciliationListTemp);
                }
                //凭证关联回单:数据组装
                if(ReconciliationDataSource.Voucher.getValue() == reconciliationdatasource){
                    bankReceiptMap.put(checkno,bankReconciliationListTemp);
                    journalReceiptMap.put(checkno,toUpdateJournalListTemp);
                }
            }
            if (toUpdateJournalList.size() == 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101314"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A2","没有符合勾对的数据") /* "没有符合勾对的数据" */);
            }

            //处理勾对
            bankBillSmartCheckService.handleJournalAndBankCheck(toUpdateJournalList,toUpdateBankReconciliationList,reconciliationdatasource);
            //组装勾对关系记录表信息
            ReconciliationInfoVO reconciliationInfoVO = bankBillSmartCheckService.handleReconciliationInfoVO(toUpdateJournalList,toUpdateBankReconciliationList,
                    ReconciliationBasisType.ManualMatching.getValue(),null,bankreconciliationscheme,reconciliationdatasource,checkno,checkdate);
            //记录勾对关系
            bankBillSmartCheckService.saveReconciliationMathRecord(Collections.singletonList(reconciliationInfoVO));

            //凭证勾对，发送回单关联消息到总账
            if(ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
                ExecutorService taskExecutor = null;
                try {
                    taskExecutor  = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1,2,200,"handleBankReceiptEvent-threadpool");
                    taskExecutor.submit(() -> {
                        try {
                            //凭证关联回单，事件发送
                            cmpCheckService.handleBankReceiptEvent(bankReceiptMap,journalReceiptMap);
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                    });
                }catch (Exception e){
                    log.error(e.getMessage(), e);
                }finally {
                    if (taskExecutor!=null){
                        taskExecutor.shutdown();
                    }
                }
            }
            reback.put("success", true);
            StringBuffer message = new StringBuffer(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A6","银行日记账已勾对笔数:") /* "银行日记账已勾对笔数:" */ + toUpdateJournalList.size() + "\r\n");
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A8","银行日记账未勾对笔数:") /* "银行日记账未勾对笔数:" */ + (journalList.size() - toUpdateJournalList.size()) + "\r\n");
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A9","对账单已勾对笔数：") /* "对账单已勾对笔数：" */ + toUpdateBankReconciliationList.size() + "\r\n");
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800AB","对账单未勾对笔数:") /* "对账单未勾对笔数:" */ + (bankReconciliationList.size() - toUpdateBankReconciliationList.size()) + "\r\n");
            reback.put("message", message);
        } catch (Exception e) {
            reback.put("success", false);
            reback.put("message", e.getMessage());
            log.error("对账失败--" + e.getMessage(),e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101318"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800B1","对账失败--") /* "对账失败--" */ + e.getMessage());
        }finally {
            JedisLockUtils.unlockDzWithOutTrace(ymsLock);
        }
        return reback;
    }


    /**
     * 银行对账取消勾对逻辑,20260130调整
     */
    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public CtmJSONObject cancelTick(CtmJSONObject jsonObject) throws Exception {
        CtmJSONObject reback = new CtmJSONObject();
        String key = jsonObject.getString(RECONCILIATIONDATASOURCEID);
        Integer reconciliationdatasource = jsonObject.getInteger(RECONCILIATIONDATASOURCE);
        Json json = new Json(CtmJSONObject.toJSONString(jsonObject.getJSONArray(JOURNALS)));
        List<Journal> journalList = Objectlizer.decode(json, Journal.ENTITY_NAME);
        json = new Json(CtmJSONObject.toJSONString(jsonObject.getJSONArray(BANKRECONCILIATIONS)));
        List<BankReconciliation> bankReconciliationList = Objectlizer.decode(json, BankReconciliation.ENTITY_NAME);
        if((journalList == null || journalList.size() == 0) && (bankReconciliationList == null || bankReconciliationList.size() == 0) ){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101319"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800B9","未选中任何数据") /* "未选中任何数据" */);
        }

        //region 共享账户，已停用对账方案明细，不可取消对账;若存在同样已启用明细，可以取消对账
        //判断银行账号+币种+授权使用组织
        String bankreconciliationscheme = jsonObject.getString(RECONCILIATIONDATASOURCEID);
        PlanParam planParam = new PlanParam(null,null,bankreconciliationscheme);
        List<BankReconciliationSettingVO> infoList = cmpCheckService.findUseOrg(planParam);
        Set<String> checkStopKey = new HashSet<>();
        Set<String> checkEnableKey = new HashSet<>();
        for (BankReconciliationSettingVO settingVO : infoList){
            if (settingVO.getEnableStatus() == EnableStatus.Disabled.getValue()){
                checkStopKey.add(settingVO.getBankAccount() + settingVO.getCurrency() + settingVO.getUseOrg());
            }
            if (settingVO.getEnableStatus() == EnableStatus.Enabled.getValue()){
                checkEnableKey.add(settingVO.getBankAccount() + settingVO.getCurrency() + settingVO.getUseOrg());
            }
        }
        //endregion

        //region 取消勾对时，数据分组和统计
        int unCheckSum = 0; //未勾兑笔数
        int sealNum = 0; //已封存数据，不能取消勾兑
        int autobillSum = 0;//自动生单笔数
        Map<String, ReconciliationInfoVO> reconciliationInfoVOMap = new HashMap<>();
        // 处理Journal列表，按checkno分组
        if (journalList != null && !journalList.isEmpty()) {
            for ( Journal journal : journalList) {
                if(!journal.getCheckflag()){
                    unCheckSum++;
                    continue;
                }
                //已封存数据不能取消勾兑
                if (journal.getSealflag()!=null && journal.getSealflag()){
                    sealNum++;
                    continue;
                }
                //如果勾兑号为空，则说明是自动生单生成的单据，自动生单生成的单据不能取消对账
                if(StringUtils.isEmpty(journal.getCheckno())){
                    autobillSum++;
                    continue;
                }
                journal.setCheckflag(false);
                journal.setCheckno(journal.getCheckno());
                journal.setBankreconciliationsettingid(key);
                journal.setDefine1(journal.get("ts"));
                String checkno = journal.getCheckno();
                if (checkno != null) {
                    reconciliationInfoVOMap.computeIfAbsent(checkno, k -> {
                        ReconciliationInfoVO vo = new ReconciliationInfoVO();
                        // 设置基础信息（从第一个journal中获取）
                        vo.setAccentity(journal.getAccentity());
                        vo.setBankaccount(journal.getBankaccount());
                        vo.setCurrency(journal.getCurrency());
                        vo.setCheckno(checkno);
                        vo.setCheckDate(journal.getCheckdate());
                        vo.setReconciliationScheme(bankreconciliationscheme);
                        vo.setReconciliationDataSource(Short.valueOf(String.valueOf(reconciliationdatasource)));
                        return vo;
                    });
                    // 添加journal到对应的列表中
                    reconciliationInfoVOMap.get(checkno).getJournalList().add(journal);
                }
            }
        }
        // 处理BankReconciliation列表，按checkno分组
        if (bankReconciliationList != null && !bankReconciliationList.isEmpty()) {
            for (BankReconciliation bankReconciliation : bankReconciliationList) {
                    //已封存数据不能取消勾兑
                if (bankReconciliation.getSealflag()){
                        sealNum++;
                        continue;
                    }
                if (ReconciliationDataSource.Voucher.getValue() == reconciliationdatasource) {
                    if (!bankReconciliation.getOther_checkflag()) {
                        unCheckSum++;
                        continue;
                    }
                }else{
                    if(!bankReconciliation.getCheckflag()){
                        unCheckSum++;
                        continue;
                    }
                }
                String checkno = null;
                // 根据数据源确定checkno字段
                if (ReconciliationDataSource.Voucher.getValue() == reconciliationdatasource) {
                    checkno = bankReconciliation.getOther_checkno();
                } else {
                    checkno = bankReconciliation.getCheckno();
                }
                if (checkno != null) {
                    String finalCheckno = checkno;
                    reconciliationInfoVOMap.computeIfAbsent(checkno, k -> {
                        ReconciliationInfoVO vo = new ReconciliationInfoVO();
                        // 设置基础信息（从第一个bankReconciliation中获取）
                        vo.setAccentity(bankReconciliation.getAccentity());
                        vo.setBankaccount(bankReconciliation.getBankaccount());
                        vo.setCurrency(bankReconciliation.getCurrency());
                        vo.setCheckno(finalCheckno);
                        vo.setReconciliationScheme(bankreconciliationscheme);
                        vo.setReconciliationDataSource(Short.valueOf(String.valueOf(reconciliationdatasource)));
                        return vo;
                    });
                    // 添加bankReconciliation到对应的列表中
                    reconciliationInfoVOMap.get(checkno).getBankReconciliationList().add(bankReconciliation);
                }
            }
        }
        //将凭证/日记账，和银行流水根据勾对号组装成勾对信息，进行后续的取消勾对操作
        List<ReconciliationInfoVO> reconciliationInfoVOList = new ArrayList<>(reconciliationInfoVOMap.values());
        //endregion 取消勾对时，数据分组和统计

        //region 多线程分批进行取消勾对
        List<String> containUnenabledBankaccount = new ArrayList<>(); //包含停用明细的，不可取消勾对
        List<String> balanceAdjustCheckMsg = new ArrayList<>(); //用来存储余额调节表校验提示
        List<String> notTotalDataList = new ArrayList<>(); //存放不是全部数据的勾对关系
        List<String> netAmountNotSameList = new ArrayList<>(); //凭证取消勾对时，凭证和银行流水净额不一致
        //存储需要取消勾对的勾对号
        List<String> successChecknoList = Collections.synchronizedList(new ArrayList<>());
        List<Journal> successJournalList = Collections.synchronizedList(new ArrayList<>());
        List<ReconciliationMatchRecord> successMatchRecordList = Collections.synchronizedList(new ArrayList<>());
        int batchSize = reconciliationInfoVOList.size() < 50 ? 1 : Math.min(10, reconciliationInfoVOList.size() / 50);
        ThreadPoolUtil.executeByBatch(executorService, reconciliationInfoVOList, batchSize, "银企对账-取消勾对批量操作", (int fromIndex, int toIndex) -> {//@notranslate
            String builder = "";
            for (int t = fromIndex; t < toIndex; t++) {
                ReconciliationInfoVO reconciliationInfoVO = reconciliationInfoVOList.get(t);
                if (checkStopKey.size() > 0 ){
                    //存在已停用银行对账方案明细关联数据，不可取消对账，请检查
                    String unenabledKey = reconciliationInfoVO.getBankaccount() + reconciliationInfoVO.getCurrency() + reconciliationInfoVO.getAccentity();
                    if (checkStopKey.contains(unenabledKey) && !checkEnableKey.contains(unenabledKey)){
                        containUnenabledBankaccount.add(reconciliationInfoVO.getCheckno());
                            continue;
                    }
                }

                //处理余额调节表是否生成校验， 取消勾对时， 根据取消勾对数据的记账日期、交易日期，判断是否可取消勾对；
                //一组取消勾对的数据中制单日期为：X ,交易日期为：Y ,当前账户已生成余额调节表的最大的对账截止日为：Z （01-31、02-28，指的是02-28）
                // max(X, Y)小于等于Z 时，校验提示：已生成余额调节表，不允许取消勾对
                List<Journal> journalToCheckList = reconciliationInfoVO.getJournalList();
                List<BankReconciliation> bankToCheckList = reconciliationInfoVO.getBankReconciliationList();
                Date maxJournalDate = null; // journalToCheckList 中的最大日期
                Date maxBankDate = null;    // bankToCheckList 中的最大日期
                // 获取 journalToCheckList 中的最大日期
                for (Journal journal : journalToCheckList) {
                    Date vouchDate;
                    if (ReconciliationDataSource.Voucher.getValue() == reconciliationdatasource){
                        vouchDate = journal.getVouchdate();
                    }else {
                        vouchDate = journal.getDzdate();
                    }
                    if (maxJournalDate == null || DateUtils.dateCompare(vouchDate, maxJournalDate) == 1) {
                        maxJournalDate = vouchDate;
                    }
                }
                // 获取 bankToCheckList 中的最大日期
                if(bankToCheckList != null){
                    for (BankReconciliation bank : bankToCheckList) {
                        Date dzDate = bank.getDzdate();
                        if (maxBankDate == null || DateUtils.dateCompare(dzDate, maxBankDate) == 1) {
                            maxBankDate = dzDate;
                        }
                    }
                }

                // 比较两个最大日期，获取全局最大值
                Date maxDate = null;
                if (maxJournalDate != null && maxBankDate != null) {
                    maxDate = DateUtils.dateCompare(maxJournalDate, maxBankDate) == 1 ? maxJournalDate : maxBankDate;
                } else if (maxJournalDate != null) {
                    maxDate = maxJournalDate;
                } else if (maxBankDate != null) {
                    maxDate = maxBankDate;
                }
                //查询该对账方案下+银行账户是否存在余额调节表的截止日期晚于等于“勾对日期”，如不存在，则正常流转
                QuerySchema querySchema = QuerySchema.create().addSelect("id");
                QueryConditionGroup group = QueryConditionGroup.and(
                    QueryCondition.name("currency").eq(reconciliationInfoVO.getCurrency()),//币种
                    QueryCondition.name("bankaccount").eq(reconciliationInfoVO.getBankaccount()),//银行账号
                        QueryCondition.name("bankreconciliationscheme").eq(bankreconciliationscheme),//对账方案id
                        QueryCondition.name("dzdate").egt(maxDate) //截止日期晚于等于勾对日期
                );
                querySchema.addCondition(group);
                List<BalanceAdjustResult> checkList =  MetaDaoHelper.queryObject(BalanceAdjustResult.ENTITY_NAME, querySchema, null);
                if(!CollectionUtils.isEmpty(checkList)){
                    balanceAdjustCheckMsg.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_216DB42E05D8002B", "勾对号【%s】关联的银行账户和币种在【%s】之后已生成余额调节表") /* "勾对号【%s】关联的银行账户和币种在【%s】之后已生成余额调节表" */,reconciliationInfoVO.getCheckno(),DateUtils.dateFormat(maxDate,"yyyy-MM-dd")));
                    continue;
                }

                //查询勾对记录关系
                List<ReconciliationMatchRecord> matchRecordList = bankBillSmartCheckService.queryReconciliationMatchRecord(Collections.singletonList(reconciliationInfoVO.getCheckno()));
                //存在勾对关系，且为凭证数据源时，校验取消勾对时所选数据是否完整
                if (matchRecordList != null && matchRecordList.size() > 0 && ReconciliationDataSource.Voucher.getValue() == reconciliationdatasource){
                    if (journalToCheckList.size() + bankToCheckList.size() != matchRecordList.size()){
                            notTotalDataList.add(reconciliationInfoVO.getCheckno());
                        continue;
                    }
                }
                //不存在勾对关系，且为凭证数据源时，校验净额是否一致
                if ((matchRecordList == null || matchRecordList.size() == 0) && ReconciliationDataSource.Voucher.getValue() == reconciliationdatasource){
                    //日记账净额
                    BigDecimal journalAmount = BigDecimal.ZERO;
                    //对账单净额
                    BigDecimal bankAmount = BigDecimal.ZERO;
                    //统计银行日记账/凭证净额
                    for (Journal j : journalToCheckList){
                        if (j.getDirection().equals(Direction.Debit)){ //借
                            journalAmount = journalAmount.add(j.getDebitoriSum());
                        } else {
                            journalAmount = journalAmount.subtract(j.getCreditoriSum());
                        }
                    }
                    //统计银行对账单净额
                    for (BankReconciliation b : bankToCheckList){
                        if (b.getDc_flag().equals(Direction.Debit)){ //借
                            if (b.getDebitamount() != null) {
                                bankAmount = bankAmount.subtract(b.getDebitamount());
                            }
                        } else {
                            if (b.getCreditamount() != null) {
                                bankAmount = bankAmount.add(b.getCreditamount());
                            }
                        }
                    }
                    //1.没有银行流水数据时，不需要校验，凭证有勾兑号，对应的流水会取消掉勾对；
                    //2.有银行流水数据，但是净额不相同，代表勾选数据不对;
                    if (bankToCheckList.size() >0 && journalAmount.compareTo(bankAmount) != 0) {
                        netAmountNotSameList.add(reconciliationInfoVO.getCheckno());
                        continue;
                    }
                }
                successChecknoList.add(reconciliationInfoVO.getCheckno());
                successJournalList.addAll(journalToCheckList);
                //删除勾对关系记录数据
                if(matchRecordList != null && matchRecordList.size() > 0){
                    successMatchRecordList.addAll(matchRecordList);
                }
            }
            return builder;
        }, false);
        //endregion

        //处理取消勾对
        if(successChecknoList.size()>0){
            Map<String,Object> param = new HashMap<>();
            String seqNo = UUID.randomUUID().toString();
            param.put("checknos",successChecknoList);
            param.put("ytenant_id", InvocationInfoProxy.getTenantid());
            if(ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
                //调用总账接口更新勾兑状态、勾兑号
                bankBillSmartCheckService.batchUpdateCheckFlag(successJournalList,seqNo);
                SqlHelper.update(DETAILPREX + ".updateOtherBankreconciliationFalse",param);
            }else{
                SqlHelper.update(DETAILPREX + ".updateJournalFalse",param);
                SqlHelper.update(DETAILPREX + ".updateBankreconciliationFalse",param);
            }
        }
        //删除勾对关系记录数据
        if(successMatchRecordList.size() > 0){
            bankBillSmartCheckService.deleteReconciliationMatchRecord(successMatchRecordList);
        }

        StringBuffer message = new StringBuffer();
        message.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_216DB42E05D80028", "取消对账成功笔数:%s笔") /* "取消对账成功笔数:%s笔" */,successChecknoList.size()) + "\r\n");
        if (containUnenabledBankaccount.size() > 0 || balanceAdjustCheckMsg.size() > 0 || notTotalDataList.size() > 0 || netAmountNotSameList.size() > 0){
            message.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_216DB42E05D80029", "失败笔数：%s笔") /* "失败笔数：%s笔" */, containUnenabledBankaccount.size() + balanceAdjustCheckMsg.size() + notTotalDataList.size() + netAmountNotSameList.size()) + "\r\n");
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_216DB42E05D8002A", "失败原因：") /* "失败原因：" */);
            if(containUnenabledBankaccount.size() > 0){
                message.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_216DB42E05D8002C", "存在银行账户和币种关联了已停用明细，勾对号：%s") /* "存在银行账户和币种关联了已停用明细，勾对号：%s" */,String.join(",",containUnenabledBankaccount)) + "\r\n");
            }
            if(balanceAdjustCheckMsg.size() > 0){
                message.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_216DB42E05D8002D", "余额调节表校验不通过：%s") /* "余额调节表校验不通过：%s" */,String.join(";\r\n",balanceAdjustCheckMsg)) + "\r\n");
            }
            if(notTotalDataList.size() > 0){
                message.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_216DB42E05D8002E", "凭证数据源取消勾对时，数据未全部选中，勾对号：%s") /* "凭证数据源取消勾对时，数据未全部选中，勾对号：%s" */,String.join(",",notTotalDataList)) + "\r\n");
            }
            if(netAmountNotSameList.size() > 0){
                message.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_216DB42E05D80030", "凭证数据源取消勾对时，净额校验不通过，勾对号：%s") /* "凭证数据源取消勾对时，净额校验不通过，勾对号：%s" */,String.join(",",netAmountNotSameList)) + "\r\n");
            }
        }
            if (unCheckSum > 0) {
                message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800AA","未勾对的不需要取消勾对!") /* "未勾对的不需要取消勾对!" */ + "\r\n");
            }
            if (autobillSum > 0) {
                message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800AC","银行对账单已自动生成收付款单据，不能取消对账，如果确需取消对账，请删除单据！") /* "银行对账单已自动生成收付款单据，不能取消对账，如果确需取消对账，请删除单据！" */ + "\r\n");
            }
            if (sealNum > 0 ){
                message.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00177", "已封存数据不能取消对账，如果确需取消对账，请先取消封存！涉及笔数：%d") /* "已封存数据不能取消对账，如果确需取消对账，请先取消封存！涉及笔数：%d" */,sealNum) + "\r\n");
            }

            reback.put("success", true);
        reback.put("message",message.toString());
        return reback;
    }

    @Override
    public String checkDzEndDate(CtmJSONObject jsonObject) throws Exception{
        String accentity = jsonObject.getString("accentity");
        String bankreconciliationscheme = jsonObject.getString("bankreconciliationscheme"); //对账方案
        String dzEndDate = jsonObject.getString("dzEndDate"); //对账截止日期
        boolean checkflag = Boolean.parseBoolean(jsonObject.getString("checkflag")); //是否勾兑

        //账户共享调整。凭证数据需要添加accbookId账簿id
        String accbookId = jsonObject.getString("accbookId");

        //单组织逻辑
        if(FIDubboUtils.isSingleOrg()){
            BizObject singleOrg = FIDubboUtils.getSingleOrg();
            if(singleOrg!=null){
                accentity = singleOrg.get("id");
            }
        }

        if (StringUtil.isEmpty(accentity)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101323"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050022", "资金组织不能为空") /* "资金组织不能为空" */);
        }
        if(checkflag){
            return ResultMessage.success();
        }
        BizObject bizObject = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME,bankreconciliationscheme);
        List<BizObject> list = bizObject.get(BANKRECONCILIATIONSETTING_B);
        List<Object> bankaccounts = new ArrayList<>();
        for(BizObject bankReconciliationSetting_b :list){
            String bankaccount = bankReconciliationSetting_b.get(ICmpConstant.BANKACCOUNT);
            bankaccounts.add(bankaccount);
        }
        Integer reconciliationdatasource = bizObject.get(RECONCILIATIONDATASOURCE);
        if(ReconciliationDataSource.BankJournal.getValue()==reconciliationdatasource){ //日记账对账
            checkDzEndDate(bankaccounts,dzEndDate);
        }else if(ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){ //凭证对账
            getMaxVoucherDate(accentity,bankreconciliationscheme,dzEndDate,bankaccounts,accbookId);
        }
        CtmJSONObject resp = new CtmJSONObject();
        String enableDate = bizObject.get(ENABLEDATE).toString();
        resp.put("reconciliationdatasource",reconciliationdatasource);
        resp.put("enableDate",enableDate);
        return ResultMessage.data(resp);
    }

    /**
     * 对账封存
     */
    @Override
    public CtmJSONObject seal(CtmJSONObject jsonObject) throws JsonProcessingException {
        CtmJSONObject reback = new CtmJSONObject();
        String key = jsonObject.getString(RECONCILIATIONDATASOURCEID);
        Integer reconciliationdatasource = jsonObject.getInteger(RECONCILIATIONDATASOURCE);
//        if (ReconciliationDataSource.Voucher.getValue() == reconciliationdatasource){
//            reback.put("success", true);
//            reback.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00178", "凭证日记账暂不支持对账封存！") /* "凭证日记账暂不支持对账封存！" */);
//            return reback;
//        }
        Json json = new Json(CtmJSONObject.toJSONString(jsonObject.getJSONArray(JOURNALS)));
        List<Journal> journalList = Objectlizer.decode(json, Journal.ENTITY_NAME);
        json = new Json(CtmJSONObject.toJSONString(jsonObject.getJSONArray(BANKRECONCILIATIONS)));
        List<BankReconciliation> bankReconciliationList = Objectlizer.decode(json, BankReconciliation.ENTITY_NAME);
        if ((journalList == null || journalList.size() == 0) && (bankReconciliationList == null || bankReconciliationList.size() == 0)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101324"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00176", "未选中任何数据") /* "未选中任何数据" */);
        }
        List<String> checknos = new ArrayList<>();
        List<Journal> toUpdateJournalList = new ArrayList<Journal>();
        int unCheckSum = 0; //未勾兑笔数
        int autobillSum = 0;
        int sealNum = 0; //已封存的数据
        if ((journalList != null && journalList.size() > 0)) {
            for (Journal journal : journalList) {
                if (!journal.getCheckflag()) {
                    unCheckSum++;
                    continue;
                }
                if (journal.getSealflag()){
                    sealNum++;
                    continue;
                }
                if (!checknos.contains(journal.getCheckno())) {
                    //如果勾兑号为空，则说明是自动生单生成的单据，自动生单生成的单据不能取消对账
                    if (StringUtils.isEmpty(journal.getCheckno())) {
                        autobillSum++;
                        continue;
                    }
                    checknos.add(journal.getCheckno());
                }
                journal.setCheckflag(false);
                journal.setCheckno(journal.getCheckno());
                journal.setBankreconciliationsettingid(key);
                journal.setDefine1(journal.get("ts"));
                toUpdateJournalList.add(journal);
            }
        }
        if (bankReconciliationList != null && bankReconciliationList.size() > 0) {
            for (BankReconciliation bank : bankReconciliationList) {
                if (ReconciliationDataSource.Voucher.getValue() == reconciliationdatasource) {
                    if (!bank.getOther_checkflag()) {
                        unCheckSum++;
                        continue;
                    }
                    if (!checknos.contains(bank.getOther_checkno())) {
                        checknos.add(bank.getOther_checkno());
                    }
                } else {
                    if (!bank.getCheckflag()) {
                        unCheckSum++;
                        continue;
                    }
                    if(bank.getSealflag()){
                        sealNum++;
                        continue;
                    }
                    if (!checknos.contains(bank.getCheckno())) {
                        //如果勾兑号为空，则说明是自动生单生成的单据，自动生单生成的单据不能取消对账
                        if (StringUtils.isEmpty(bank.getCheckno())) {
                            autobillSum++;
                            continue;
                        }
                        checknos.add(bank.getCheckno());
                    }
                }
            }
        }
        try {
            if (ValueUtils.isNotEmpty(checknos)) {
                Map<String, Object> param = new HashMap<>();
                param.put("checknos", checknos);
                param.put("ytenant_id", InvocationInfoProxy.getTenantid());
                if (ReconciliationDataSource.Voucher.getValue() == reconciliationdatasource) {
                    //todo 凭证日记账封存
                    //调用总账接口更新封存状态
                    batchUpdateSealFlag(toUpdateJournalList,true);
                    SqlHelper.update(DETAILPREX + ".updateOtherBankreconciliationSealTrue", param);
                } else {
                    SqlHelper.update(DETAILPREX + ".updateJournalSealTrue", param);
                    SqlHelper.update(DETAILPREX + ".updateBankreconciliationSealTrue", param);
                }
                //勾对关系记录表做封存处理
                List<ReconciliationMatchRecord> matchRecordList = bankBillSmartCheckService.queryReconciliationMatchRecord(checknos);
                if (matchRecordList != null && matchRecordList.size() > 0){
                    bankBillSmartCheckService.sealReconciliationMatchRecord(matchRecordList,true);
                }
            }

            StringBuffer message = new StringBuffer(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0016F", "对账封存成功笔数：%d") /* "对账封存成功笔数：%d" */,checknos.size()) + "\r\n");
            if (unCheckSum > 0) {
                message.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00171", "未勾兑的数据不能封存，涉及笔数：%d") /* "未勾兑的数据不能封存，涉及笔数：%d" */,unCheckSum) + "\r\n");
            }
            if (sealNum > 0) {
                message.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00173", "已封存的不需要进行封存，涉及笔数:%d") /* "已封存的不需要进行封存，涉及笔数:%d" */,sealNum)+ "\r\n");
            }
            if (autobillSum > 0) {
                message.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00175", "银行对账单已自动生成收付款单据，不能取消对账，无需封存，涉及笔数:%d") /* "银行对账单已自动生成收付款单据，不能取消对账，无需封存，涉及笔数:%d" */,autobillSum)+ "\r\n");
            }

            reback.put("success", true);
            reback.put("message", message);
        } catch (Exception e) {
            reback.put("success", false);
            reback.put("message", e.getMessage());
            log.error("对账封存失败" + e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101325"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0017A", "对账封存失败！") /* "对账封存失败！" */ + e.getMessage());
        }
        return reback;
    }

    /**
     * 取消封存
     */
    @Override
    public CtmJSONObject cancelSeal(CtmJSONObject jsonObject) throws JsonProcessingException {
        CtmJSONObject reback = new CtmJSONObject();
        String key = jsonObject.getString(RECONCILIATIONDATASOURCEID);
        Integer reconciliationdatasource = jsonObject.getInteger(RECONCILIATIONDATASOURCE);
//        if (ReconciliationDataSource.Voucher.getValue() == reconciliationdatasource){
//            reback.put("success", true);
//            reback.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00170", "凭证日记账暂不支持取消封存！") /* "凭证日记账暂不支持取消封存！" */);
//            return reback;
//        }
        Json json = new Json(CtmJSONObject.toJSONString(jsonObject.getJSONArray(JOURNALS)));
        List<Journal> journalList = Objectlizer.decode(json, Journal.ENTITY_NAME);
        json = new Json(CtmJSONObject.toJSONString(jsonObject.getJSONArray(BANKRECONCILIATIONS)));
        List<BankReconciliation> bankReconciliationList = Objectlizer.decode(json, BankReconciliation.ENTITY_NAME);
        if ((journalList == null || journalList.size() == 0) && (bankReconciliationList == null || bankReconciliationList.size() == 0)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101324"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00176", "未选中任何数据") /* "未选中任何数据" */);
        }
        List<String> checknos = new ArrayList<>();
        List<Journal> toUpdateJournalList = new ArrayList<Journal>();
        int unSealSum = 0; //未封存笔数
        if ((journalList != null && journalList.size() > 0)) {
            for (Journal journal : journalList) {
                if (!journal.getSealflag()) {
                    unSealSum++;
                    continue;
                }
                if (!checknos.contains(journal.getCheckno())) {
                    checknos.add(journal.getCheckno());
                }
                journal.setCheckflag(false);
                journal.setCheckno(journal.getCheckno());
                journal.setBankreconciliationsettingid(key);
                journal.setDefine1(journal.get("ts"));
                toUpdateJournalList.add(journal);
            }
        }
        if (bankReconciliationList != null && bankReconciliationList.size() > 0) {
            for (BankReconciliation bank : bankReconciliationList) {
                if (ReconciliationDataSource.Voucher.getValue() == reconciliationdatasource) {
                    //todo 凭证对账封存
                    if (!bank.getSealflag()) {
                        unSealSum++;
                        continue;
                    }
                    if (!checknos.contains(bank.getOther_checkno())) {
                        checknos.add(bank.getOther_checkno());
                    }
                } else {
                    if (!bank.getSealflag()) {
                        unSealSum++;
                        continue;
                    }
                    if (!checknos.contains(bank.getCheckno())) {
                        checknos.add(bank.getCheckno());
                    }
                }
            }
        }
        try {
            if (ValueUtils.isNotEmpty(checknos)) {
                Map<String, Object> param = new HashMap<>();
                param.put("checknos", checknos);
                param.put("ytenant_id", InvocationInfoProxy.getTenantid());
                if (ReconciliationDataSource.Voucher.getValue() == reconciliationdatasource) {
                    //调用总账接口更新封存状态
                    batchUpdateSealFlag(toUpdateJournalList,false);
                    SqlHelper.update(DETAILPREX + ".updateOtherBankreconciliationSealFalse", param);
                } else {
                    SqlHelper.update(DETAILPREX + ".updateJournalSealFalse", param);
                    SqlHelper.update(DETAILPREX + ".updateBankreconciliationSealFalse", param);
                }
                //勾对关系记录表做封存处理
                List<ReconciliationMatchRecord> matchRecordList = bankBillSmartCheckService.queryReconciliationMatchRecord(checknos);
                if (matchRecordList != null && matchRecordList.size() > 0){
                    bankBillSmartCheckService.sealReconciliationMatchRecord(matchRecordList,false);
                }
            }

            StringBuffer message = new StringBuffer(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00172", "取消封存成功笔数：%d") /* "取消封存成功笔数：%d" */,checknos.size()) + "\r\n");
            if (unSealSum > 0) {
                message.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00174", "未封存的数据不能取消封存，涉及笔数：%d") /* "未封存的数据不能取消封存，涉及笔数：%d" */,unSealSum) + "\r\n");
            }

            reback.put("success", true);
            reback.put("message", message);
        } catch (Exception e) {
            reback.put("success", false);
            reback.put("message", e.getMessage());
            log.error("取消封存失败" + e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101326"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00179", "取消封存失败！") /* "取消封存失败！" */ + e.getMessage());
        }
        return reback;
    }

    /**
     *  净额对账
     */
    @Override
    public CtmJSONObject netamountTick(CtmJSONObject jsonObject) throws Exception {
        CtmJSONObject reback = new CtmJSONObject();
        String key = jsonObject.getString(RECONCILIATIONDATASOURCEID);
        Integer reconciliationdatasource = jsonObject.getInteger(RECONCILIATIONDATASOURCE);
        Json json = new Json(CtmJSONObject.toJSONString(jsonObject.getJSONArray(JOURNALS)));
        List<Journal> journalList = Objectlizer.decode(json, Journal.ENTITY_NAME);
        json = new Json(CtmJSONObject.toJSONString(jsonObject.getJSONArray(BANKRECONCILIATIONS)));
        List<BankReconciliation> bankReconciliationList = Objectlizer.decode(json, BankReconciliation.ENTITY_NAME);
        if ((journalList == null || journalList.size() == 0) && (bankReconciliationList == null || bankReconciliationList.size() == 0))
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101312"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800B5","未勾选任何数据") /* "未勾选任何数据" */);
        if(bankReconciliationList == null || bankReconciliationList.size() == 0){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101316"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800BB","银行对账单不能为空") /* "银行对账单不能为空" */);
        }
        if(journalList == null || journalList.size() == 0){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101317"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800BD","企业日记账不能为空") /* "企业日记账不能为空" */);
        }
        //对账方案明细停用，后台不支持勾对；明细可能存在同样的数据但启停用状态不一致，存在停用数据但是也有启用数据则跳过该校验
        String bankreconciliationscheme = jsonObject.getString(RECONCILIATIONDATASOURCEID);
        //校验银行账号
        checkBankAccounts(bankreconciliationscheme,journalList,bankReconciliationList);

        //账户共享，左侧日记账授权使用组织集合
        Set<String> journalAccentitySet = new HashSet<>();
        //对账单全部授权使用组织合集
        Set<String> allAccentitySet = new HashSet<>();
        Set<String> accountAndCurrencySet = new HashSet<>();
        for (Journal j : journalList){
            if (!StringUtils.isEmpty(j.getAccentity())){
                journalAccentitySet.add(j.getAccentity());
                allAccentitySet.add(j.getAccentity());
            }else {
                journalAccentitySet.add("");
                allAccentitySet.add("");
            }
            accountAndCurrencySet.add(j.getBankaccount() + j.getCurrency());
        }
        for (BankReconciliation b : bankReconciliationList){
            if (!StringUtils.isEmpty(b.getAccentity())){
                allAccentitySet.add(b.getAccentity());
            }else {
                allAccentitySet.add("");
            }
            accountAndCurrencySet.add(b.getBankaccount() + b.getCurrency());
        }
        //账户共享校验左侧勾选数据中，授权使用组织是否一致，如不一致，提示：”勾选数据中存在不同的授权使用组织，不允许同时勾对，请检查！“
        //校验左侧与右侧勾选数据中，授权使用组织是否一致(左侧有值，右侧为空时默认一致)，如不一致，提示：”勾选数据中存在不同的授权使用组织，不允许同时勾对，请检查！“
        if(ReconciliationDataSource.BankJournal.getValue() == reconciliationdatasource  && (journalAccentitySet.size() > 1 || allAccentitySet.size() > 1)){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F3C48A20508000A", "勾选数据中存在不同的账户使用组织，不允许同时勾对，请检查！") /* "勾选数据中存在不同的账户使用组织，不允许同时勾对，请检查！" */);
        }

        //净额对账需要保证账号和币种相同
        if (accountAndCurrencySet.size() > 1){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101327"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1DBABB540590002C", "需勾选同一账号、币种的数据，请检查！") /* "需勾选同一账号、币种的数据，请检查！" */);
        }

        //检查组织锁是否存在
        YmsLock ymsLock = null;
        try {
            //加锁
            ymsLock = JedisLockUtils.lockDzWithOutTrace(key);
            if (ymsLock == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101302"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800CC","该组织下正在银行对账，请稍后再试！") /* "该组织下正在银行对账，请稍后再试！" */);
            }
            Long checkman = queryOperator();
            //勾对时间
            Date date = BillInfoUtils.getBusinessDate();//业务日期
            Date checkdate = date == null? new Date() : date;
            //勾对号;20260130需求调整为 NR + 勾对业务日期 + 19位OID
            String checkno = "NR" + DateUtils.parseDateToStr(checkdate,"yyyyMMdd") + "-" + ymsOidGenerator.nextStrId();

            //日记账净额
            BigDecimal journalAmount = BigDecimal.ZERO;
            //对账单净额
            BigDecimal bankAmount = BigDecimal.ZERO;

            //统计银行日记账/凭证净额
            for (Journal j : journalList){
                if (j.getDirection().equals(Direction.Debit)){ //借
                    journalAmount = journalAmount.add(j.getDebitoriSum());
                } else {
                    journalAmount = journalAmount.subtract(j.getCreditoriSum());
                }
                j.setDefine1(j.get("ts"));
                packBill(null, j, checkno,checkdate, true, checkman, SettleStatus.alreadySettled,reconciliationdatasource,key);
            }
            //统计银行对账单净额
            for (BankReconciliation b : bankReconciliationList){
                if (b.getDc_flag().equals(Direction.Debit)){ //借
                    bankAmount = bankAmount.subtract(b.getDebitamount());
                } else {
                    bankAmount = bankAmount.add(b.getCreditamount());
                }
                packBill(b, null, checkno,checkdate, true, checkman, null,reconciliationdatasource,key);
            }

            //需要净额统计相同
            if (journalAmount.compareTo(bankAmount) != 0) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_216DB42E05D8002F", "净额不相同，不满足勾对条件") /* "净额不相同，不满足勾对条件" */);
            }
            //凭证勾兑回单文件需求
            //key：勾兑号 value:勾兑的对账单集合
            Map<String,List<BankReconciliation>> bankReceiptMap = new HashMap<>();
            //key：勾兑号 value:勾兑的凭证集合
            Map<String,List<Journal>> journalReceiptMap = new HashMap<>();

            //凭证关联回单:数据组装
            if(ReconciliationDataSource.Voucher.getValue() == reconciliationdatasource){
                bankReceiptMap.put(checkno,bankReconciliationList);
                journalReceiptMap.put(checkno,journalList);
            }
            //凭证勾对
            bankBillSmartCheckService.handleJournalAndBankCheck(journalList,bankReconciliationList,reconciliationdatasource);
            //组装勾对关系记录表信息
            ReconciliationInfoVO reconciliationInfoVO = bankBillSmartCheckService.handleReconciliationInfoVO(journalList,bankReconciliationList,
                    ReconciliationBasisType.NetAmountMatching.getValue(),null,bankreconciliationscheme,reconciliationdatasource,checkno,checkdate);
            //记录勾对关系
            bankBillSmartCheckService.saveReconciliationMathRecord(Collections.singletonList(reconciliationInfoVO));

            //凭证勾对，发送回单关联消息到总账
            if(ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
                ExecutorService taskExecutor = null;
                try {
                    taskExecutor  = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(1,2,200,"handleBankReceiptEvent-threadpool");
                    taskExecutor.submit(() -> {
                        try {
                            //凭证关联回单，事件发送
                            cmpCheckService.handleBankReceiptEvent(bankReceiptMap,journalReceiptMap);
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                    });
                }catch (Exception e){
                    log.error(e.getMessage(), e);
                }finally {
                    if (taskExecutor!=null){
                        taskExecutor.shutdown();
                    }
                }
            }
            reback.put("success", true);
            StringBuffer message = new StringBuffer(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A6","银行日记账已勾对笔数:") /* "银行日记账已勾对笔数:" */ + journalList.size() + "\r\n");
            message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A9","对账单已勾对笔数：") /* "对账单已勾对笔数：" */ + bankReconciliationList.size() + "\r\n");
            reback.put("message", message);
        } catch (Exception e) {
            reback.put("success", false);
            reback.put("message", e.getMessage());
            log.error("对账失败--" + e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101318"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800B1","对账失败--") /* "对账失败--" */ + e.getMessage());
        }finally {
            JedisLockUtils.unlockDzWithOutTrace(ymsLock);
        }
        return reback;
    }

    public void packBill(BankReconciliation bank, Journal journal, String checkno,
            Date date, Boolean flag,Long checkman, SettleStatus settleStatus,
                         Integer reconciliationdatasource,String reconciliationdatasourceid){
        //勾对时间赋值
        Date checkTime = null;
        try {
            checkTime = DateUtils.parseDate(DateUtils.dateFormat(date,"yyyy-MM-dd")+ DateUtils.dateFormat(new Date()," HH:mm:ss"),"yyyy-MM-dd HH:mm:ss");
        }catch (Exception e){
            log.error("银企对账勾对赋值 packBill 日期转换异常");
        }
        if (bank != null){
            if(ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
                bank.setOther_checkflag(flag);
                bank.setOther_checkno(checkno);
                bank.setOther_checkdate(date);
                bank.setOther_checktime(checkTime);
                bank.setGl_bankreconciliationsettingid(reconciliationdatasourceid);
            }else{
                bank.setCheckno(checkno);
                bank.setCheckflag(flag);
                bank.setCheckdate(date);
                bank.setChecktime(checkTime);
                bank.setCheckman(checkman);
                bank.setBankreconciliationsettingid(reconciliationdatasourceid);
            }
            bank.setEntityStatus(EntityStatus.Update);
        }
        if (journal != null){
            journal.setCheckflag(flag);
            journal.setCheckno(checkno);
            journal.setCheckdate(date);
            journal.setChecktime(checkTime);
            journal.setCheckman(checkman);
            journal.setBankreconciliationsettingid(reconciliationdatasourceid);
            if(null!=journal.getSrcbillitemid()){
                journal.setSettlestatus(settleStatus);
            }
            journal.setEntityStatus(EntityStatus.Update);
        }
    }

    /**
     * 更新收款单--结算标识
     * @param ids
     */
    public void updateRecvSettle(List<Long> ids){
        try {
            if (ids.size() > 0){
                String fields = "id,pubts";
                List<Map<String, Object>> list = MetaDaoHelper.queryByIds(ReceiveBill.ENTITY_NAME, fields, ids.toArray(new Long[ids.size()]));

                List<ReceiveBill> receives = new ArrayList<>();
                for (Map<String, Object> map : list){
                    ReceiveBill re = new ReceiveBill();
                    re.setId(map.get("id"));
                    re.setPubts((Date) map.get("pubts"));
                    re.setSettlestatus(SettleStatus.alreadySettled);
                    re.setEntityStatus(EntityStatus.Update);

                    receives.add(re);
                }
                MetaDaoHelper.update(ReceiveBill.ENTITY_NAME, receives);
            }
        } catch (Exception e) {
            log.error("更新收款单失败--" + e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101328"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A4","更新收款单失败") /* "更新收款单失败" */ + e.getMessage());
        }
    }


    private  Map<String,List<Journal>>  groupByJournalList(List<Journal> dataList){
        List<Journal> dataListTemp = null ;
        Map<String,List<Journal>> accounts = new HashMap<String,List<Journal>>();
        for (Journal data : dataList){
            String bankAccount = data.getBankaccount();
            if(!accounts.containsKey(bankAccount)){
                dataListTemp = new ArrayList<Journal>();
            }else {
                dataListTemp = accounts.get(bankAccount);
            }
            dataListTemp.add(data);
            accounts.put(bankAccount,dataListTemp);
        }
        return accounts;
    }

    private  Map<String,List<BankReconciliation>>  groupByBankReconciliationList(List<BankReconciliation> dataList){
        List<BankReconciliation> dataListTemp = null ;
        Map<String,List<BankReconciliation>> accounts = new HashMap<String,List<BankReconciliation>>();
        for (BankReconciliation data : dataList){
            String bankAccount = data.getBankaccount();
            if(!accounts.containsKey(bankAccount)){
                dataListTemp = new ArrayList<BankReconciliation>();
            }else {
                dataListTemp = accounts.get(bankAccount);
            }
            dataListTemp.add(data);
            accounts.put(bankAccount,dataListTemp);
        }
        return accounts;
    }

    private Long queryOperator() throws  Exception {
        return  AppContext.getCurrentUser().getId();
    }


    private  Map<String,Object> journalCheck(List<Journal> journalListTemp,Map<String,Object> param){
        Map<String,Object> result = new HashMap<String,Object>();
        Direction direction = journalListTemp.get(0).getDirection();
        String leftCurrency = journalListTemp.get(0).getCurrency();
        List<Journal> toUpdateJournalListTemp =  new ArrayList<Journal>();
        //List<Long> toUpdateIdsTemp = new ArrayList<>();
        List<Long> receiveBillIds = new ArrayList<>();
        List<Long> payMentIds = new ArrayList<>();
        List<Long> transferAccountIds = new ArrayList<>();
        List<Long> currencyExchangeBillIds = new ArrayList<>();
        List<Long> salaryPaymentIds = new ArrayList<>();
        BigDecimal leftAmount = BigDecimal.ZERO;
        boolean  isError = false;
        Long currUserId = AppContext.getCurrentUser().getId();
        for(Journal journal : journalListTemp){
            if(!leftCurrency.equals(journal.getCurrency())){
                isError = true;
                break;
            }
            if (!direction.equals(journal.getDirection())){
                isError = true;
                break;
            }
            //如果是借方
            if (direction.equals(Direction.Debit)){
                leftAmount = BigDecimalUtils.safeAdd(leftAmount, journal.getDebitoriSum());
            } else {
                leftAmount = BigDecimalUtils.safeAdd(leftAmount, journal.getCreditoriSum());
            }
            Journal journalNew = new Journal();
            journalNew.setId(journal.getId());
            journalNew.setSrcbillitemid(journal.getSrcbillitemid());
            journalNew.setBankreconciliationsettingid((String) param.get("bankreconciliationsettingid"));
            journalNew.setTradetype(journal.getTradetype());
            journalNew.setDefine1(journal.get("ts"));
            /*if (journal.getRptype() != null && journal.getRptype().equals(RpType.ReceiveBill)&&journal.getAuditstatus().getValue() == AuditStatus.Complete.getValue()){
                toUpdateIdsTemp.add(Long.valueOf(journal.getSrcbillitemid()));
            }*/
            if(ReconciliationDataSource.BankJournal.getValue() == Short.parseShort(param.get("reconciliationdatasource").toString())){
                // addSettleStatusAndGenVoucherId(receiveBillIds, journal, EventType.ReceiveBill.getValue());
                // addSettleStatusAndGenVoucherId(payMentIds, journal, EventType.PayMent.getValue());
                // addSettleStatusAndGenVoucherId(transferAccountIds, journal, EventType.TransferAccount.getValue());
                // addSettleStatusAndGenVoucherId(currencyExchangeBillIds, journal,EventType.CurrencyExchangeBill.getValue());
                // addSettleStatusAndGenVoucherId(salaryPaymentIds, journal, EventType.SalaryPayment.getValue());
            }
            packBill(null, journalNew, (String) param.get("checkno"), (Date) param.get("checkdate"), true,  currUserId, SettleStatus.alreadySettled,null,(String) param.get("bankreconciliationsettingid"));
            toUpdateJournalListTemp.add(journalNew);
        }
        result.put("isError",isError);
        result.put("leftAmount",leftAmount);
        result.put("toUpdateJournalListTemp",toUpdateJournalListTemp);
        //result.put("toUpdateIdsTemp",toUpdateIdsTemp);
        result.put("receiveBillIds", receiveBillIds);
        result.put("payMentIds", payMentIds);
        result.put("transferAccountIds", transferAccountIds);
        result.put("currencyExchangeBillIds", currencyExchangeBillIds);
        result.put("salaryPaymentIds", salaryPaymentIds);
        return  result;
    }

    private  Map<String,Object> bankReconciliationCheck(List<BankReconciliation> bankReconciliationListTemp,Map<String,Object> param){
        Map<String,Object> result = new HashMap<String,Object>();
        BigDecimal rightAmount = BigDecimal.ZERO;
        Integer reconciliationdatasource = (Integer) param.get("reconciliationdatasource");
        for(BankReconciliation bank : bankReconciliationListTemp){
            if(ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
                if(bank.getOther_checkflag()){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101303"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A1","已经勾对的不能再次勾对!") /* "已经勾对的不能再次勾对!" */);
                }
            }else{
                if(bank.getCheckflag()){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101303"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A1","已经勾对的不能再次勾对!") /* "已经勾对的不能再次勾对!" */);
                }
            }
            if(!param.get("leftCurrency").equals(bank.getCurrency())){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105057"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2294C2A404380004", "币种不一致，不允许做勾对！") /* "币种不一致，不允许做勾对！" */ );
            }
            // 日记账金额大于等于0
            if (param.get("leftAmount") != null && ((BigDecimal)param.get("leftAmount")).compareTo(BigDecimal.ZERO) >= 0) {
                if (param.get("direction").equals(bank.getDc_flag())){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105058"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2294C2A404380005", "收支方向不一致，不允许做手工勾对！若净额一致，请使用净额对账。") /* "收支方向不一致，不允许做手工勾对！若净额一致，请使用净额对账。" */);
                }
                if (param.get("direction").equals(Direction.Debit)){
                    rightAmount = BigDecimalUtils.safeAdd(rightAmount, bank.getCreditamount());
                } else {
                    rightAmount = BigDecimalUtils.safeAdd(rightAmount, bank.getDebitamount());
                }
            } else if (param.get("leftAmount") != null && ((BigDecimal)param.get("leftAmount")).compareTo(BigDecimal.ZERO) < 0) {
                // 金额为负数时，取方向相同的金额
                if (param.get("direction").equals(Direction.Debit)){
                    if (bank.getDebitamount()!=null && bank.getDebitamount().compareTo(BigDecimal.ZERO) > 0){
                        rightAmount = BigDecimalUtils.safeAdd(rightAmount, bank.getDebitamount());
                    } else {
                        //20250530 手工对账支持两边同时为负数对账调整
                        rightAmount = BigDecimalUtils.safeAdd(rightAmount, bank.getCreditamount());
                    }
                } else {
                    if (bank.getCreditamount() !=null && bank.getCreditamount().compareTo(BigDecimal.ZERO) > 0){
                        rightAmount = BigDecimalUtils.safeAdd(rightAmount, bank.getCreditamount());
                    }else {
                        //20250530 手工对账支持两边同时为负数对账调整
                        rightAmount = BigDecimalUtils.safeAdd(rightAmount, bank.getDebitamount());
                    }
                }
            }
            packBill(bank, null, (String) param.get("checkno"),(Date) param.get("checkdate") , true, (Long) param.get("checkman"), null,(Integer)param.get("reconciliationdatasource"),(String) param.get("bankreconciliationsettingid"));
        }
        result.put("rightAmount",rightAmount.abs());
        result.put("bankReconciliationListTemp",bankReconciliationListTemp);
        return  result;
    }

    /**
     * 调用总账接口更新勾兑状态
     * 202412调整为RPC接口
     * @param journals
     */
    private void batchUpdateCheckFlag(List<Journal> journals){
        if(journals==null||journals.size()==0){
            return;
        }
        //总账勾对/取消勾对接口
        CashVoucherCheckInfoDTO cashVoucherCheckInfoDTO = new CashVoucherCheckInfoDTO();
        cashVoucherCheckInfoDTO.setCheckflag(journals.get(0).getCheckflag());
        List<CheckInfoDTO> checkInfoDTOList = new ArrayList<>();
        for(Journal journal:journals){
            CheckInfoDTO checkInfoDTO = new CheckInfoDTO();
            checkInfoDTO.setVoucherbid(journal.getSrcbillitemid());
            checkInfoDTO.setTradetype(journal.getTradetype());
            checkInfoDTO.setCheckno(journal.getCheckno());
            checkInfoDTO.setBankreconciliationsettingid(journal.getBankreconciliationsettingid());
            checkInfoDTO.setTs(journal.getDefine1());
            checkInfoDTOList.add(checkInfoDTO);
            //记录业务日志
            CtmJSONObject logparam = new CtmJSONObject();
            logparam.put("vouchinfo",journal);
            logparam.put("checkno",journal.getCheckno());
            ctmcmpBusinessLogService.saveBusinessLog(logparam, journals.get(0).getCheckno(), "银企对账-凭证数据勾对准备", IServicecodeConstant.BANKRECONCILIATION, "银企对账", "现金银行流水勾对/取消勾对的的凭证数据");//@notranslate
        }
        cashVoucherCheckInfoDTO.setCheckinfo(checkInfoDTOList);
        ResultDataDTO result = RemoteDubbo.get(IVoucherBankRpcService.class, "yonbip-fi-gl").updateCheckFlagTry(cashVoucherCheckInfoDTO);
        //记录业务日志
        CtmJSONObject logparam = new CtmJSONObject();
        logparam.put("requestVo",cashVoucherCheckInfoDTO);
        logparam.put("resultVo",result);
        ctmcmpBusinessLogService.saveBusinessLog(logparam, "", "现金调用总账凭证勾对接口", IServicecodeConstant.BANKRECONCILIATION, "银企对账", "现金调用总账凭证勾对接口");//@notranslate
        if(!"200".equals(result.getCode())){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101330"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A5","更新总账凭证勾兑状态失败，总账报错：") /* "更新总账凭证勾兑状态失败，总账报错：" */ + result.getMessage());
        }
    }


    /**
     * 批量更新凭证日记账封存状态
     * @param journals
     */
    private void batchUpdateSealFlag(List<Journal> journals,Boolean sealFlag) throws JsonProcessingException {
        if (journals == null || journals.size() == 0) {
            return;
        }
        Boolean successFlag = false;
        List<CtmJSONObject> dataList = new ArrayList<>();
        for (Journal journal : journals) {
            CtmJSONObject data = new CtmJSONObject();
            data.put("voucherBodyId", journal.getSrcbillitemid());
            //用来区分数据是凭证还是期初未达
            data.put("tradetype",journal.getTradetype());
            data.put("sealFlag", sealFlag?1:0);
            dataList.add(data);
        }
        String serverUrl = AppContext.getEnvConfig("yzb.base.url");
        String BASE_URL_ACCOUNT_SETTLE = serverUrl + "/voucher/voucherUpdateSealFlag";
        String thd_userId = AppContext.getCurrentUser().getYhtUserId();
        Map<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");
        header.put("thd_userId", thd_userId);
        String str = HttpTookit.
                doPostWithJson(BASE_URL_ACCOUNT_SETTLE, JsonUtils.toJson(dataList), header, "UTF-8");
        CtmJSONObject result = CtmJSONObject.parseObject(str);
        successFlag = (Boolean) result.get("success");
        if (!successFlag) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101331"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_186D3B1A0470001B","更新凭证日记账封存状态报错，报错信息：") /* "更新凭证日记账封存状态报错，报错信息：" */ + result.get("message"));
        }
    }

    /**
     * 校验对账截止日期
     * @param bankaccounts
     * @param dzEndDate
     * @throws Exception
     */
    private void checkDzEndDate(List<Object> bankaccounts,String dzEndDate) throws Exception{
        //查询日记账已勾兑的最大登账日期
        QuerySchema queryJournal = QuerySchema.create().addSelect("max(dzdate)");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("bankaccount").in(bankaccounts),
                QueryCondition.name("checkflag").eq(true));
        queryJournal.addCondition(group);
        Map<String, Object> journalMax = MetaDaoHelper.queryOne(Journal.ENTITY_NAME,queryJournal);
        if(journalMax==null||journalMax.get("max")==null){
            journalMax = new HashMap<>();
            journalMax.put("max",DateUtils.strToDate(dzEndDate));
        }
        //查询对账单已勾兑的最大交易日期
        QuerySchema queryReconciliation = QuerySchema.create().addSelect("max(tran_date)");
        queryReconciliation.addCondition(group);
        Map<String, Object> reconciliationMax = MetaDaoHelper.queryOne(BankReconciliation.ENTITY_NAME,queryReconciliation);
        if(journalMax==null||journalMax.get("max")==null){
            journalMax = new HashMap<>();
            journalMax.put("max",DateUtils.strToDate(dzEndDate));
        }
        if(reconciliationMax==null||reconciliationMax.get("max")==null){
            reconciliationMax = new HashMap<>();
            reconciliationMax.put("max",DateUtils.strToDate(dzEndDate));
        }
        if(journalMax!=null&&journalMax.get("max")!=null&&reconciliationMax!=null&&reconciliationMax.get("max")!=null){
            Date journalDate = (Date)journalMax.get("max"); //日记账已勾兑最大日期
            Date checkDate = (Date)reconciliationMax.get("max"); //对账单已勾兑最大日期

            Date maxJzDate = null;  //最大结账日——取（最大日记账已勾兑最大对账日期）和（对账单已勾兑最大交易日期）中的最大者
            if(DateUtils.dateCompare(journalDate,checkDate)==-1){
                maxJzDate = checkDate;
            }else{
                maxJzDate = journalDate;
            }
            //比较对账截止日期和最大结账日
            if(DateUtils.dateCompare(DateUtils.strToDate(dzEndDate),maxJzDate)==-1){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101332"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800B4","所选择的对账截止日期不能早于当前对账日期 %s") /* "所选择的对账截止日期不能早于当前对账日期 %s" */, DateUtils.dateToStr(maxJzDate)));
            }
        }
    }

    /**
     * 调用总账接口获取对账方案的最大凭证日期
     * @param accentity 会计主体
     * @param bankreconciliationscheme 对账方案
     * @param accbookId 账簿id
     */
    private void getMaxVoucherDate(String accentity,String bankreconciliationscheme,String dzEndDate,List<Object> bankaccounts,String accbookId) throws Exception{
        CtmJSONObject jsonobject = new CtmJSONObject();
        jsonobject.put("accentity",accentity);
        jsonobject.put("bankreconciliationscheme",bankreconciliationscheme);
        jsonobject.put("accbookId",accbookId);
        String serverUrl = AppContext.getEnvConfig("yzb.base.url");
        String BASE_URL_ACCOUNT_SETTLE = serverUrl + "/accountInTransit/queryMaxMakeTime";
        String thd_userId = AppContext.getCurrentUser().getYhtUserId();
        Map<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");
        header.put("thd_userId", thd_userId);
        String str = HttpTookit.
                doPostWithJson(BASE_URL_ACCOUNT_SETTLE, CtmJSONObject.toJSONString(jsonobject), header,"UTF-8");
        CtmJSONObject result = CtmJSONObject.parseObject(str);
        Integer code = result.getInteger("code");
        String message = result.getString("message");
        if (code == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101333"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800BC","查询总账失败，总账接口返回错误信息：") /* "查询总账失败，总账接口返回错误信息：" */ + message);
        }
        //查询对账单已勾兑的最大交易日期
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("bankaccount").in(bankaccounts),
                QueryCondition.name("other_checkflag").eq(true));
        QuerySchema queryReconciliation = QuerySchema.create().addSelect("max(tran_date)");
        queryReconciliation.addCondition(group);
        Map<String, Object> reconciliationMax = MetaDaoHelper.queryOne(BankReconciliation.ENTITY_NAME,queryReconciliation);
        String maxVoucherDate = (String)result.get("maxVoucherDate");
        if(ValueUtils.isNotEmpty(maxVoucherDate)&&reconciliationMax!=null&&reconciliationMax.get("max")!=null){
            Date checkDate = (Date)reconciliationMax.get("max"); //对账单已勾兑最大日期
            Date journalDate = DateUtils.dateParse(maxVoucherDate, DateUtils.pattern);
            Date maxJzDate = null;  //最大结账日——取（最大日记账已勾兑最大对账日期）和（对账单已勾兑最大交易日期）中的最大者
            if(DateUtils.dateCompare(journalDate,checkDate)==-1){
                maxJzDate = checkDate;
            }else{
                maxJzDate = journalDate;
            }
            if(DateUtils.dateCompare(DateUtils.strToDate(dzEndDate),maxJzDate)==-1){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101332"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800B4","所选择的对账截止日期不能早于当前对账日期 %s") /* "所选择的对账截止日期不能早于当前对账日期 %s" */, DateUtils.dateToStr(maxJzDate)));
            }
        }
    }


    /**
     * 做对账的时候，1，校验是否存在已停用关联的银行账户,2.校验银行账户是否在对应的对账方案明细里
     * @param bankreconciliationscheme
     * @param journalList
     * @param bankReconciliationList
     * @throws Exception
     */
    private void checkBankAccounts(String bankreconciliationscheme,List<Journal> journalList,List<BankReconciliation> bankReconciliationList) throws Exception{
        PlanParam planParam = new PlanParam(null,null,bankreconciliationscheme);
        List<BankReconciliationSettingVO> infoList = cmpBankReconciliationSettingRpcService.findUseOrg(planParam);
        Set<String> checkStopKey = new HashSet<>();
        Set<String> checkEnableKey = new HashSet<>();
        Set<String> enableBankAccounts = new HashSet<>();
        for (BankReconciliationSettingVO settingVO : infoList){
            if (settingVO.getEnableStatus() == EnableStatus.Disabled.getValue()){
                checkStopKey.add(settingVO.getBankAccount() + settingVO.getCurrency() + settingVO.getUseOrg());
            }
            if (settingVO.getEnableStatus() == EnableStatus.Enabled.getValue()){
                checkEnableKey.add(settingVO.getBankAccount() + settingVO.getCurrency() + settingVO.getUseOrg());
                enableBankAccounts.add(settingVO.getBankAccount());
            }
        }
        if (checkStopKey.size() > 0 ){
            if (journalList != null && journalList.size() > 0){
                for (Journal j : journalList){
                    if (checkStopKey.contains(j.getBankaccount() + j.getCurrency() + j.getAccentity()) && !checkEnableKey.contains(j.getBankaccount() + j.getCurrency() + j.getAccentity()) ){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101301"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B54469C05A0001F","存在已停用银行对账方案明细关联数据，不允许进行勾对操作，请检查！") /* "存在已停用银行对账方案明细关联数据，不允许进行勾对操作，请检查！" */);
                    }
                }
            }
            if (bankReconciliationList != null && bankReconciliationList.size() > 0){
                for (BankReconciliation b : bankReconciliationList){
                    if (checkStopKey.contains(b.getBankaccount() + b.getCurrency() + b.getAccentity()) && !checkEnableKey.contains(b.getBankaccount() + b.getCurrency() + b.getAccentity()) ){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101301"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B54469C05A0001F","存在已停用银行对账方案明细关联数据，不允许进行勾对操作，请检查！") /* "存在已停用银行对账方案明细关联数据，不允许进行勾对操作，请检查！" */);
                    }
                }
            }
        }
        //校验银行账号是否在对账方案明细里
        if (enableBankAccounts.size() > 0 ){
            if (journalList != null && journalList.size() > 0){
                for (Journal j : journalList){
                    if (!enableBankAccounts.contains(j.getBankaccount())){
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20B0136604A0000E", "数据中存在不匹配的银行账号，不可进行对账") /* "数据中存在不匹配的银行账号，不可进行对账" */);
                    }
                }
            }
            if (bankReconciliationList != null && bankReconciliationList.size() > 0){
                for (BankReconciliation b : bankReconciliationList){
                    if (!enableBankAccounts.contains(b.getBankaccount()) ){
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20B0136604A0000E", "数据中存在不匹配的银行账号，不可进行对账") /* "数据中存在不匹配的银行账号，不可进行对账" */);
                    }
                }
            }
        }
    }

    /**
     * 调用总账接口更新勾兑状态 （rest回环调整为RPC接口，方法废弃，作为备份）
     * @param journals
     */
    private void batchUpdateCheckFlagByRest(List<Journal> journals){
        if(journals==null||journals.size()==0){
            return;
        }
        Boolean successFlag = false;
        CtmJSONObject jsonobject = new CtmJSONObject();
        List<Map<String,Object>> dataList = new ArrayList<Map<String,Object>>();
        jsonobject.put("checkflag",journals.get(0).getCheckflag());
        for(Journal journal:journals){
            Map<String,Object> data = new HashMap<String,Object>();
            data.put("voucherbid",journal.getSrcbillitemid());
            data.put("tradetype",journal.getTradetype());
            data.put("checkno",journal.getCheckno());
            data.put("bankreconciliationsettingid",journal.getBankreconciliationsettingid());
            data.put("ts",journal.getDefine1());
            dataList.add(data);
        }
        jsonobject.put("checkinfo",dataList);
        String serverUrl = AppContext.getEnvConfig("yzb.base.url");
//        String BASE_URL_ACCOUNT_SETTLE = serverUrl + "/cash/updatecheckflag";
        //202403适配YTS分布式
        String BASE_URL_ACCOUNT_SETTLE = serverUrl + "/cash/updatecheckflagTry";
        String thd_userId = AppContext.getCurrentUser().getYhtUserId();
        Map<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");
        header.put("thd_userId", thd_userId);
        //202403适配YTS分布式
        header.put("ytsEnable", "true");
        header.put("ytsMode", "tcc");
        String str = HttpTookitYts.
                doPostWithJson(BASE_URL_ACCOUNT_SETTLE, CtmJSONObject.toJSONString(jsonobject), header,"UTF-8");
//        String str = HttpTookit.
//                doPostWithJson(BASE_URL_ACCOUNT_SETTLE, CtmJSONObject.toJSONString(jsonobject), header,"UTF-8");
        CtmJSONObject result = CtmJSONObject.parseObject(str);
//        successFlag = (Boolean)result.get("success");
//        if(!successFlag){
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101330"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A5","更新总账凭证勾兑状态失败，总账报错：") /* "更新总账凭证勾兑状态失败，总账报错：" */ + result.get("message"));
//        }
        //适配yts 200成功；500失败
        String code = result.getString("code");
        if(!"200".equals(code)){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101330"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A5","更新总账凭证勾兑状态失败，总账报错：") /* "更新总账凭证勾兑状态失败，总账报错：" */ + result.get("message"));
        }
    }

    /**
     * 银行对账取消勾对逻辑(20260130版本，重写了取消勾对方法，该方法废弃作为备份)
     */
    private CtmJSONObject cancelTickOld_Feiqi(CtmJSONObject jsonObject) throws Exception {
        CtmJSONObject reback = new CtmJSONObject();
        String key = jsonObject.getString(RECONCILIATIONDATASOURCEID);
        Integer reconciliationdatasource = jsonObject.getInteger(RECONCILIATIONDATASOURCE);
        Json json = new Json(CtmJSONObject.toJSONString(jsonObject.getJSONArray(JOURNALS)));
        List<Journal> journalList = Objectlizer.decode(json, Journal.ENTITY_NAME);
        json = new Json(CtmJSONObject.toJSONString(jsonObject.getJSONArray(BANKRECONCILIATIONS)));
        List<BankReconciliation> bankReconciliationList = Objectlizer.decode(json, BankReconciliation.ENTITY_NAME);
        if((journalList == null || journalList.size() == 0) && (bankReconciliationList == null || bankReconciliationList.size() == 0) ){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101319"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800B9","未选中任何数据") /* "未选中任何数据" */);
        }

        //共享账户，已停用对账方案明细，不可取消对账;若存在同样已启用明细，可以取消对账
        //判断银行账号+币种+授权使用组织
        String bankreconciliationscheme = jsonObject.getString(RECONCILIATIONDATASOURCEID);
        PlanParam planParam = new PlanParam(null,null,bankreconciliationscheme);
        List<BankReconciliationSettingVO> infoList = cmpCheckService.findUseOrg(planParam);
        Set<String> checkStopKey = new HashSet<>();
        Set<String> checkEnableKey = new HashSet<>();
        for (BankReconciliationSettingVO settingVO : infoList){
            if (settingVO.getEnableStatus() == EnableStatus.Disabled.getValue()){
                checkStopKey.add(settingVO.getBankAccount() + settingVO.getCurrency() + settingVO.getUseOrg());
            }
            if (settingVO.getEnableStatus() == EnableStatus.Enabled.getValue()){
                checkEnableKey.add(settingVO.getBankAccount() + settingVO.getCurrency() + settingVO.getUseOrg());
            }
        }
        if (checkStopKey.size() > 0 ){
            if (journalList != null && journalList.size() > 0){
                for (Journal j : journalList){
                    if (checkStopKey.contains(j.getBankaccount() + j.getCurrency() + j.getAccentity()) && !checkEnableKey.contains(j.getBankaccount() + j.getCurrency() + j.getAccentity())){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101320"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B54469C05A00021","存在已停用银行对账方案明细关联数据，不可取消对账，请检查！") /* "存在已停用银行对账方案明细关联数据，不可取消对账，请检查！" */);
                    }
                }
            }
            if (bankReconciliationList != null && bankReconciliationList.size() > 0){
                for (BankReconciliation b : bankReconciliationList){
                    if (checkStopKey.contains(b.getBankaccount() + b.getCurrency() + b.getAccentity()) && !checkEnableKey.contains(b.getBankaccount() + b.getCurrency() + b.getAccentity())){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101320"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B54469C05A00021","存在已停用银行对账方案明细关联数据，不可取消对账，请检查！") /* "存在已停用银行对账方案明细关联数据，不可取消对账，请检查！" */);
                    }
                }
            }
        }

        // 20250530迭代需求 生成余额调节表后，余额调节表之前的数据不允许取消勾对，需增加此校验，当前校验没校验住
        // 处理校验是否已生成余额调节表的数据 ，key=勾兑号
        Map<String, List<BankReconciliation>> bankReconciliationToCheckMap = new HashMap<>();
        Map<String, List<Journal>> journalToCheckMap = new HashMap<>();
        List<String> checknos = new ArrayList<>();
        List<Journal> toUpdateJournalList = new ArrayList<Journal>();
        int unCheckSum = 0; //未勾兑笔数
        int sealNum = 0; //已封存数据，不能取消勾兑
        int autobillSum = 0;//自动生单笔数
        if((journalList != null && journalList.size() > 0)){
            for ( Journal journal : journalList) {
                if(!journal.getCheckflag()){
                    unCheckSum++;
                    continue;
                }
                //已封存数据不能取消勾兑
                if (journal.getSealflag()!=null && journal.getSealflag()){
                    sealNum++;
                    continue;
                }
                if (!checknos.contains(journal.getCheckno())){
                    //如果勾兑号为空，则说明是自动生单生成的单据，自动生单生成的单据不能取消对账
                    if(StringUtils.isEmpty(journal.getCheckno())){
                        autobillSum++;
                        continue;
                    }
                    checknos.add(journal.getCheckno());
                }
                String other_checkno = journal.getCheckno();
                if (journalToCheckMap.containsKey(other_checkno)){
                    journalToCheckMap.get(other_checkno).add(journal);
                }else {
                    List<Journal> jList = new ArrayList<>();
                    jList.add(journal);
                    journalToCheckMap.put(other_checkno,jList);
                }
                journal.setCheckflag(false);
                journal.setCheckno(journal.getCheckno());
                journal.setBankreconciliationsettingid(key);
                journal.setDefine1(journal.get("ts"));
                toUpdateJournalList.add(journal);
            }
        }
        if (bankReconciliationList != null && bankReconciliationList.size() > 0) {
            //处理单边勾对取消集合存放，key=勾兑号
            Map<String, List<BankReconciliation>> bankReconciliationMap = new HashMap<>();
            for (BankReconciliation bank : bankReconciliationList) {
                if (ReconciliationDataSource.Voucher.getValue() == reconciliationdatasource) {
                    //已封存数据不能取消勾兑
                    if (bank.getSealflag()){
                        sealNum++;
                        continue;
                    }
                    if (!bank.getOther_checkflag()) {
                        unCheckSum++;
                        continue;
                    }
                    //用来存放校验余额调节表生成情况的数据，key=总账勾对号
                    String other_checkno = bank.getOther_checkno();
                    if (bankReconciliationToCheckMap.containsKey(other_checkno)){
                        bankReconciliationToCheckMap.get(other_checkno).add(bank);
                    }else {
                        List<BankReconciliation> bList = new ArrayList<>();
                        bList.add(bank);
                        bankReconciliationToCheckMap.put(other_checkno,bList);
                    }
                    //CZFW-387511 银行对账单单边对账问题处理
                    if(checknos.size() == 0){ //在凭证勾兑号为空时，再考虑单边勾对
                        if (bankReconciliationMap.containsKey(other_checkno)){
                            bankReconciliationMap.get(other_checkno).add(bank);
                        }else {
                            List<BankReconciliation> bList = new ArrayList<>();
                            bList.add(bank);
                            bankReconciliationMap.put(other_checkno,bList);
                        }
                    }
                }else{
                    if(!bank.getCheckflag()){
                        unCheckSum++;
                        continue;
                    }
                    //已封存数据不能取消勾兑,凭证的去掉判断
                    if (bank.getSealflag()!=null && bank.getSealflag()){
                        sealNum++;
                        continue;
                    }
                    if (!checknos.contains(bank.getCheckno())){
                        //如果勾兑号为空，则说明是自动生单生成的单据，自动生单生成的单据不能取消对账
                        if(StringUtils.isEmpty(bank.getCheckno())){
                            autobillSum++;
                            continue;
                        }
                        checknos.add(bank.getCheckno());
                    }
                    //用来存放校验余额调节表生成情况的数据，key=日记账勾对号
                    String other_checkno = bank.getCheckno();
                    if (bankReconciliationToCheckMap.containsKey(other_checkno)){
                        bankReconciliationToCheckMap.get(other_checkno).add(bank);
                    }else {
                        List<BankReconciliation> bList = new ArrayList<>();
                        bList.add(bank);
                        bankReconciliationToCheckMap.put(other_checkno,bList);
                    }
                }

            }
            //单边勾对取消处理
            if (bankReconciliationMap.size() > 0){
                for (Map.Entry<String, List<BankReconciliation>> entry : bankReconciliationMap.entrySet()) {
                    String bankCheckNo = entry.getKey();
                    List<BankReconciliation> bankList = entry.getValue();
                    //茅台存在单边一条金额为0的场景，去除该限制
//                    if (bankList.size() == 1) {
//                        continue;
//                    }
                    //单边勾对金额判断
                    BigDecimal amount = BigDecimal.ZERO;
                    for (BankReconciliation bank : bankList) {
                        Direction direction = bank.getDc_flag();
                        if (direction != null && direction.equals(Direction.Debit)) {
                            amount = BigDecimalUtils.safeAdd(amount, bank.getDebitamount());
                        } else {
                            amount = BigDecimalUtils.safeSubtract(false, amount, bank.getCreditamount());
                        }
                    }
                    //金额相同，则代表是单边勾对
                    if (amount.compareTo(BigDecimal.ZERO) == 0){
                        if (!checknos.contains(bankCheckNo)){
                            checknos.add(bankCheckNo);
                        }
                    }
                }
            }
        }
        //处理余额调节表是否生成校验， 取消勾对时， 根据取消勾对数据的制单日期、交易日期，判断是否可取消勾对；
        //一组取消勾对的数据中制单日期为：X ,交易日期为：Y ,当前账户已生成余额调节表的最大的对账截止日为：Z （01-31、02-28，指的是02-28）
        // max(X, Y)小于等于Z 时，校验提示：已生成余额调节表，不允许取消勾对
        //包含凭证或者日记账
        Set<String> checkedSet = new HashSet<>();
        if (journalToCheckMap.size() > 0){
            for (Map.Entry<String, List<Journal>> entry : journalToCheckMap.entrySet()){
                if (!checknos.contains(entry.getKey())){ //不在取消勾对里不做判断
                    continue;
                }
                String checkno = entry.getKey();
                checkedSet.add(checkno);
                List<Journal> journalToCheckList = entry.getValue();
                List<BankReconciliation> bankToCheckList = bankReconciliationToCheckMap.get(checkno);
                Date maxJournalDate = null; // journalToCheckList 中的最大日期
                Date maxBankDate = null;    // bankToCheckList 中的最大日期
                // 获取 journalToCheckList 中的最大日期
                for (Journal journal : journalToCheckList) {
                    Date vouchDate = journal.getVouchdate();
                    if (maxJournalDate == null || DateUtils.dateCompare(vouchDate, maxJournalDate) == 1) {
                        maxJournalDate = vouchDate;
                    }
                }
                // 获取 bankToCheckList 中的最大日期
                if(bankToCheckList != null){
                    for (BankReconciliation bank : bankToCheckList) {
                        Date dzDate = bank.getDzdate();
                        if (maxBankDate == null || DateUtils.dateCompare(dzDate, maxBankDate) == 1) {
                            maxBankDate = dzDate;
                        }
                    }
                }

                // 比较两个最大日期，获取全局最大值
                Date maxDate = null;
                if (maxJournalDate != null && maxBankDate != null) {
                    maxDate = DateUtils.dateCompare(maxJournalDate, maxBankDate) == 1 ? maxJournalDate : maxBankDate;
                } else if (maxJournalDate != null) {
                    maxDate = maxJournalDate;
                } else if (maxBankDate != null) {
                    maxDate = maxBankDate;
                }
                //查询该对账方案下+银行账户是否存在余额调节表的截止日期晚于等于“勾对日期”，如不存在，则正常流转
                if (maxDate != null){
                    Journal j = journalToCheckList.get(0);
                    QuerySchema querySchema = QuerySchema.create().addSelect("id");
                    QueryConditionGroup group = QueryConditionGroup.and(
                            QueryCondition.name("currency").eq(j.getCurrency()),//币种
                            QueryCondition.name("bankaccount").eq(j.getBankaccount()),//银行账号
                            QueryCondition.name("bankreconciliationscheme").eq(bankreconciliationscheme),//对账方案id
                            QueryCondition.name("dzdate").egt(maxDate) //截止日期晚于等于勾对日期
                    );
                    querySchema.addCondition(group);
                    List<BalanceAdjustResult> checkList =  MetaDaoHelper.queryObject(BalanceAdjustResult.ENTITY_NAME, querySchema, null);
                    if(!CollectionUtils.isEmpty(checkList)){
                        throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F3144B00528000C", "[%s]之后已生成余额调节表，不允许取消勾对，请检查！") /* "[%s]之后已生成余额调节表，不允许取消勾对，请检查！" */, DateUtils.dateFormat(maxDate,"yyyy-MM-dd")));
                    }
                }
            }
        }
        if (bankReconciliationToCheckMap.size() > 0) {
            for (Map.Entry<String, List<BankReconciliation>> entry : bankReconciliationToCheckMap.entrySet()) {
                if (!checknos.contains(entry.getKey()) || checkedSet.contains(entry.getKey())){ //不在取消勾对里或者日记账已经判断过跳过判断
                    continue;
                }
                List<BankReconciliation> bankToCheckList = entry.getValue();
                Date maxBankDate = null;    // bankToCheckList 中的最大日期
                // 获取 bankToCheckList 中的最大日期
                for (BankReconciliation bank : bankToCheckList) {
                    Date dzDate = bank.getDzdate();
                    if (maxBankDate == null || DateUtils.dateCompare(dzDate, maxBankDate) == 1) {
                        maxBankDate = dzDate;
                    }
                }
                BankReconciliation b = bankToCheckList.get(0);
                QuerySchema querySchema = QuerySchema.create().addSelect("id");
                QueryConditionGroup group = QueryConditionGroup.and(
                        QueryCondition.name("currency").eq(b.getCurrency()),//币种
                        QueryCondition.name("bankaccount").eq(b.getBankaccount()),//银行账号
                        QueryCondition.name("bankreconciliationscheme").eq(bankreconciliationscheme),//对账方案id
                        QueryCondition.name("dzdate").egt(maxBankDate) //截止日期晚于等于勾对日期
                );
                querySchema.addCondition(group);
                List<BalanceAdjustResult> checkList =  MetaDaoHelper.queryObject(BalanceAdjustResult.ENTITY_NAME, querySchema, null);
                if(!CollectionUtils.isEmpty(checkList)){
                    throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F3144B00528000C", "[%s]之后已生成余额调节表，不允许取消勾对，请检查！") /* "[%s]之后已生成余额调节表，不允许取消勾对，请检查！" */, DateUtils.dateFormat(maxBankDate,"yyyy-MM-dd")));
                }
            }
        }
        try {
            if(ValueUtils.isNotEmpty(checknos)){
                Map<String,Object> param = new HashMap<>();
                String seqNo = UUID.randomUUID().toString();
//                for(String checkno:checknos){
//                    //记录业务日志
//                    CtmJSONObject logparam = new CtmJSONObject();
//                    logparam.put("bankreconciliationinfolist",bankReconciliationList);
//                    logparam.put("journalinfo",journalList);
//                    logparam.put("checkno",checkno);
//                    logparam.put("checkSeqNo",seqNo);
//                    ctmcmpBusinessLogService.saveBusinessLog(logparam, checkno, "银企对账-取消勾对", IServicecodeConstant.BANKRECONCILIATION, "银企对账", "取消勾对操作");//@notranslate
//                }
                param.put("checknos",checknos);
                param.put("ytenant_id", InvocationInfoProxy.getTenantid());
                if(ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
                    //调用总账接口更新勾兑状态、勾兑号
                    bankBillSmartCheckService.batchUpdateCheckFlag(toUpdateJournalList,seqNo);
                    SqlHelper.update(DETAILPREX + ".updateOtherBankreconciliationFalse",param);
                }else{
                    SqlHelper.update(DETAILPREX + ".updateJournalFalse",param);
                    SqlHelper.update(DETAILPREX + ".updateBankreconciliationFalse",param);
                }
            }

            StringBuffer message = new StringBuffer(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800A7","取消对账成功笔数：") /* "取消对账成功笔数：" */ + checknos.size() + "\r\n");
            if (unCheckSum > 0) {
                message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800AA","未勾对的不需要取消勾对!") /* "未勾对的不需要取消勾对!" */ + "\r\n");
            }
            if (autobillSum > 0) {
                message.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800AC","银行对账单已自动生成收付款单据，不能取消对账，如果确需取消对账，请删除单据！") /* "银行对账单已自动生成收付款单据，不能取消对账，如果确需取消对账，请删除单据！" */ + "\r\n");
            }
            if (sealNum > 0 ){
                message.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00177", "已封存数据不能取消对账，如果确需取消对账，请先取消封存！涉及笔数：%d") /* "已封存数据不能取消对账，如果确需取消对账，请先取消封存！涉及笔数：%d" */,sealNum) + "\r\n");
            }

            reback.put("success", true);
            reback.put("message",message);
        } catch (Exception e) {
            reback.put("success", false);
            reback.put("message", e.getMessage());
            log.error("取消对账失败" + e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101322"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800AE","取消对账失败--") /* "取消对账失败--" */ + e.getMessage());
        }
        return reback;
    }
}
