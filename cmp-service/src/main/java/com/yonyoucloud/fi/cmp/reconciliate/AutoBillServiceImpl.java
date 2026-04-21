package com.yonyoucloud.fi.cmp.reconciliate;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodQueryParam;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.uap.billcode.BillCodeObj;
import com.yonyou.uap.billcode.service.IBillCodeComponentService;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.model.ResultList;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.arap.HttpTookit;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.voucher.enums.Status;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyoucloud.fi.arap.service.account.IArapSettleAccountService;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.CurrencyUtil;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.paybill.PayBillb;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill_b;
import com.yonyoucloud.fi.cmp.receivebill.service.YtsReceiveBillServiceImpl;
import com.yonyoucloud.fi.cmp.settlementdetail.SettlementDetail;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.imeta.biz.base.Objectlizer;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.base.Json;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自动生单
 *
 * @author dongjch 2019年9月16日
 */
@Service
@Slf4j
public class AutoBillServiceImpl implements AutoBillService {
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    private static final String TRANSTYPE_URL = "trantype.url";
    private static final String BILLTYPE_ID = "billtype_id";
    private static final String BANKRECONCILIATIONS = "bankreconciliations";
    private static final String JOURNALS = "journals";
    private static final String FICM1 = "FICM1";
    private static final String FICM2 = "FICM2";
    private static final String ARAP_RECEIPT_SALE = "arap_receipt_sale";

    private static Map<String, ReceiveBill> map = new HashMap<> ();
    @Autowired
    BaseRefRpcService baseRefRpcService;


    private static final String CASHIER_ROBOT =  "P_YS_FI_CM_0001023776" /* "出纳机器人" */;
    private static  String RECONCILIATIONDATASOURCE = "reconciliationdatasource";//对账数据源

    private final Set <String> bankids = new HashSet<> ();

    private static ConcurrentHashMap < String,String> periodaccentity= new ConcurrentHashMap<>();
    private final Set <String> periodaccentitys = new HashSet<> ();

    private static ConcurrentHashMap < String,String> periodaccentity_ap= new ConcurrentHashMap<>();
    private final Set <String> periodaccentitys_ap = new HashSet<> ();

    private static ConcurrentHashMap < String,Object> customaccentity= new ConcurrentHashMap<>();

    private final Set <String> customaccentitys = new HashSet<> ();

    @Autowired
    JournalService journalService;
    @Autowired
    CmpVoucherService cmpVoucherService;
    @Autowired
    CmCommonService cmCommonService;
    @Autowired
    YtsReceiveBillServiceImpl ytsReceiveBillServiceImpl;
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;
    @Autowired
    protected void setCtmcmpBusinessLogService(CTMCMPBusinessLogService ctmcmpBusinessLogService) {
        this.ctmcmpBusinessLogService = ctmcmpBusinessLogService;
    }

    @Override
    public ObjectNode autoGenerateBill(JsonNode jsonObject) throws Exception {
        Integer reconciliationdatasource = jsonObject.get(RECONCILIATIONDATASOURCE).asInt();
        ArrayNode journalArray = jsonObject.withArray(JOURNALS);
        if (journalArray != null && !journalArray.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100131"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418085D","自动生单只能为银行对账单未勾对数据") /* "自动生单只能为银行对账单未勾对数据" */);
        }
        Json json = new Json(jsonObject.withArray(BANKRECONCILIATIONS).toString());
        List<BankReconciliation> bankReconciliationLists = Objectlizer.decode(json, BankReconciliation.ENTITY_NAME);

        if (bankReconciliationLists == null || bankReconciliationLists.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100132"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026231") /* "银行对账单未勾选数据" */);
        }
        // 检查组织锁是否存在
        JedisLockUtils.isexistRjLock(bankReconciliationLists.get(0).getAccentity());
        // 检查当前日期是否日结
        checkSettle(bankReconciliationLists.get(0));
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace("autoGenerateBill" + bankReconciliationLists.get(0).getAccentity());
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100133"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050002", "当前资金组织上次自动生单流程未结束") /* "当前资金组织上次自动生单流程未结束" */);
        }
        ResultList resultList = new ResultList(bankReconciliationLists.size());
        try {
            generalBill(bankReconciliationLists,resultList,reconciliationdatasource);
        }finally {
            for (BankReconciliation bank:bankReconciliationLists){
                bankids.remove(bank.getId());
            }
            Iterator<String> iterator = customaccentitys.iterator();
            while (iterator.hasNext()){
                customaccentity.remove(iterator.next());
            }
            Iterator<String> iterator1 = periodaccentitys.iterator();
            while (iterator1.hasNext()){
                periodaccentity.remove(iterator1.next());
            }
            Iterator<String> iterator2 = periodaccentitys_ap.iterator();
            while (iterator2.hasNext()){
                periodaccentity_ap.remove(iterator2.next());
            }
            customaccentitys.clear();
            periodaccentitys.clear();
            periodaccentitys_ap.clear();
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        String message = "";
        if (bankReconciliationLists.size()==1){
            if (resultList.getMessages().size()>0){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100134"),resultList.getMessages().get(0).toString());
            } else {
                message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180852","自动生单成功") /* "自动生单成功" */;
            }
        }else {
            StringBuilder sbu = new StringBuilder();
            sbu.append(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180857","总共") /* "总共" */ + bankReconciliationLists.size() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180855","张单据,") /* "张单据," */ + resultList.getSucessCount() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180854","张生单成功，") /* "张生单成功，" */ + resultList.getFailCount() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180856","张生单失败") /* "张生单失败" */);
            for (Object onk : resultList.getMessages()) {
                sbu.append("\r\n").append(onk.toString());
            }
            message = sbu.toString();
        }
        ObjectNode obj = JSONBuilderUtils.createJson();
        obj.put("message", message);
        return obj;
    }

    /**
     * 统计生单成功和失败笔数
     *
     * @param bank
     */
/*    private CtmJSONObject countBill(BankReconciliation bank) {
        CtmJSONObject obj = new CtmJSONObject();
        try {
            // 自动生单
            List<BankReconciliation> bankReconciliations = new ArrayList<>();
            bankReconciliations.add(bank);
            this.generalBill(bankReconciliations);
            obj.put("flag", true);
        } catch (Exception e) {
            log.error("generalBill--exception:", e);
            obj.put("flag", false);
            obj.put("errMsg", e.getMessage());
        }
        return obj;
    }*/

    /**
     * 检查当前日期是否日结
     *
     * @param bankReconciliation
     * @throws Exception
     */
    private void checkSettle(BankReconciliation bankReconciliation) throws Exception {
        QueryConditionGroup condition = new QueryConditionGroup();
        condition.addCondition(QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(bankReconciliation.getAccentity())));
        condition.addCondition(QueryConditionGroup.and(QueryCondition.name("accountdate").eq(DateUtils.strToDate(DateUtils.dateToStr(bankReconciliation.getTran_date())))));
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        querySchema.addCondition(condition);
        List<Map<String, Object>> settlementDetailList = MetaDaoHelper.query(SettlementDetail.ENTITY_NAME, querySchema);
        if (CollectionUtils.isNotEmpty(settlementDetailList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100135"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180863","当前日期已经日结，不允许新增单据") /* "当前日期已经日结，不允许新增单据" */);
        }
    }

    /**
     * 处理自动生单
     *
     * @param bankReconciliationLists
     * @throws Exception
     */
    private void generalBill(List<BankReconciliation> bankReconciliationLists, ResultList resultList, Integer reconciliationdatasource) throws Exception {
        List<BankReconciliation> bankReconciliationList_tem = new ArrayList<>();

        for (BankReconciliation bankReconciliation : bankReconciliationLists) {
            if (bankids.contains(bankReconciliation.getId())) {
                continue;
            }
            if (ReconciliationDataSource.Voucher.getValue() == reconciliationdatasource) {
                if (bankReconciliation.getOther_checkflag()) {
                    continue;
                }
            } else {
                if (bankReconciliation.getCheckflag()) {
                    continue;
                }
            }
            // 自动生成付款单  暂不支持 Direction.Debit
            if (Direction.Debit == bankReconciliation.getDc_flag()) {
                continue;
            }
            // 判断是否已生单
            if (Objects.equals(true, bankReconciliation.getAutobill())) {
                continue;
            }
            //判断是否期初，期初数据不参与生单
            if (bankReconciliation.getInitflag()) {
                continue;
            }
            bankReconciliationList_tem.add(bankReconciliation);
        }
        Map<String, Object> defaultExchangeRateType = null;
        Long accBookTypeByAccBody = null;
        AutoConfig defaultConfig = new AutoConfig();
        Map<String, Object> periodMap = new HashMap<>();
        int serviceAttr = 0;
        CtmJSONObject transTypeJson = null;
        List<ReceiveBill> receiveBillList = new ArrayList<>();
        List<PayBill> payBillList = null;

        if (!bankReconciliationList_tem.isEmpty()) {
            receiveBillList = new ArrayList<>(bankReconciliationList_tem.size());
            payBillList = new ArrayList<>(bankReconciliationList_tem.size());
            defaultExchangeRateType = cmCommonService.getDefaultExchangeRateType(bankReconciliationLists.get(0).getAccentity());
            if (defaultExchangeRateType.isEmpty() || defaultExchangeRateType.get("id") == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100136"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050001", "此资金组织对应会计主体下无默认汇率类型，请手动输入!") /* "此资金组织对应会计主体下无默认汇率类型，请手动输入!" */);
            }
            // 读取配置参数
            QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
            querySchema.addCondition(QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(bankReconciliationLists.get(0).getAccentity())));
            List<Map<String, Object>> autoConfigList = MetaDaoHelper.query(AutoConfig.ENTITY_NAME, querySchema);
            if (CollectionUtils.isEmpty(autoConfigList)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100137"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_CTM_CM-BE_0001587833") /* "未配置现金参数，请配置" */);
            }
            if (autoConfigList.get(0).get("settlemode") == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100138"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180864","自动化参数未配置结算方式") /* "自动化参数未配置结算方式" */);
            }
            // 读取配置参数
            defaultConfig.init(autoConfigList.get(0));

            serviceAttr = getServiceAttr(defaultConfig);
            if (serviceAttr != 0 && serviceAttr != 1) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100139"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026121") /* "结算方式业务属性只能为银行业务与现金业务！" */);
            }
            periodMap = getPeriod(bankReconciliationLists.get(0).getAccentity(), "AR");
            Map<String, String> paramMap = new HashMap<>();
            paramMap.put(BILLTYPE_ID, FICM1);
            paramMap.put("btoken", InvocationInfoProxy.getYhtAccessToken());
            String transTypeUrl = AppContext.getAppConfig().getProperty(TRANSTYPE_URL);
            String transTypeResult = HttpTookit.doGet(transTypeUrl, paramMap);
            if (StringUtils.isEmpty(transTypeResult)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100140"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026250") /* "默认交易类型查无数据" */);
            }
            transTypeJson = CtmJSONObject.parseObject(transTypeResult);
            if (transTypeJson.get("data") == null || transTypeJson.getJSONArray("data").isEmpty()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100141"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000026250") /* "默认交易类型查无数据" */);
            }
        }

        Long[] ids = new Long[bankReconciliationLists.size()];
        for (int i = 0; i < bankReconciliationLists.size(); i++) {
            ids[i] = bankReconciliationLists.get(i).getId();
        }
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        querySchema.addCondition(queryConditionGroup);
        bankReconciliationLists = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);

        for (BankReconciliation bankReconciliation : bankReconciliationLists) {

            if (!bankids.add(bankReconciliation.getId().toString())) {
                resultList.incFailCount();
                resultList.addMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180859","生单中") /* "生单中" */);
                continue;
            }
            if (ReconciliationDataSource.Voucher.getValue() == reconciliationdatasource) {
                if (bankReconciliation.getOther_checkflag()) {
                    resultList.incFailCount();
                    resultList.addMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418085D","自动生单只能为银行对账单未勾对数据") /* "自动生单只能为银行对账单未勾对数据" */);
                    continue;
                }
            } else {
                if (bankReconciliation.getCheckflag()) {
                    resultList.incFailCount();
                    resultList.addMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418085D","自动生单只能为银行对账单未勾对数据") /* "自动生单只能为银行对账单未勾对数据" */);
                    continue;
                }
            }
            // 自动生成付款单  暂不支持 Direction.Debit
            if (Direction.Debit == bankReconciliation.getDc_flag()) {
                resultList.incFailCount();
                resultList.addMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180862","自动生成付款单,暂不支持") /* "自动生成付款单,暂不支持" */);
                continue;
            }
            // 判断是否已生单
            if (Objects.equals(true, bankReconciliation.getAutobill())) {
                resultList.incFailCount();
                resultList.addMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180865","当前单据已经生单成功，不允许重复生单") /* "当前单据已经生单成功，不允许重复生单" */);
                continue;
            }
            //判断是否期初，期初数据不参与生单
            if (bankReconciliation.getInitflag()) {
                resultList.incFailCount();
                resultList.addMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180867","期初单据不能自动生单!") /* "期初单据不能自动生单!" */);
                continue;
            }
            try {
                createBill(receiveBillList, payBillList, bankReconciliation, defaultConfig, serviceAttr, defaultExchangeRateType, accBookTypeByAccBody, resultList, transTypeJson, periodMap);
            } catch (Exception e) {
                bankReconciliationList_tem.remove(bankReconciliation);
                log.error(e.getMessage(), e);
                resultList.incFailCount();
                resultList.addMessage(e.getMessage());
            }
        }
        // TODO 自动核销,读取配置参数

        // 收款单   生成凭证 入库 登记日记账
        if (CollectionUtils.isNotEmpty(receiveBillList) && !bankReconciliationList_tem.isEmpty()) {
            for (ReceiveBill receiveBill : receiveBillList) {
                //TransactionStatus status= SqlHelper.getTransactionStatus();
                try {
                    if (CaObject.Customer == receiveBill.getCaobject()) {
                        fiarap4SettleAlready(receiveBill, "AR");
                    } else {
                        fiarap4SettleAlready(receiveBill, "AP");
                    }
//                    receiveBill.setEntityStatus(EntityStatus.Insert);
//                    List<ReceiveBill_b> ReceiveBill_bList = receiveBill.getBizObjects("ReceiveBill_b",ReceiveBill_b.class);
//                    ReceiveBill_bList.stream().forEach(bb ->{
//                        bb.setEntityStatus(EntityStatus.Insert);
//                    });
                    log.error("AutoBillServiceImpl receiveBill ====================：" + CtmJSONObject.toJSON(receiveBill).toString() + receiveBill + ":" + receiveBill.getCaobject() + ":" + receiveBill.getBilltype());
                    if (MapUtils.isNotEmpty(periodMap) && periodMap.get("begindate") != null
                            && null != receiveBill.getCaobject() && !receiveBill.getCaobject().equals(CaObject.Other)) {
                        log.error("AutoBillServiceImpl receiveBill Caobject====================：" + receiveBill.getCaobject());
                        // 请求应收服务
                        BizObject bizObject = ytsReceiveBillServiceImpl.syncBill(receiveBill);
                        Journal receiveBillJournal = createJournal(receiveBill, Direction.Debit);
                        journalService.compute4Save(receiveBill, receiveBillJournal, Direction.Debit);
                        bizObject.set("_entityName", ReceiveBill.ENTITY_NAME);
                        //cmpVoucherService.generateVoucherWithResult(bizObject);
                    } else {
                        log.error("AutoBillServiceImpl receiveBill Manual Caobject====================：" + receiveBill.getCaobject());
                        CmpMetaDaoHelper.insert(ReceiveBill.ENTITY_NAME, receiveBill);
                        Journal receiveBillJournal = createJournal(receiveBill, Direction.Debit);
                        journalService.compute4Save(receiveBill, receiveBillJournal, Direction.Debit);
                        ReceiveBill receiveBills = MetaDaoHelper.findById(ReceiveBill.ENTITY_NAME, receiveBill.getId());
                        receiveBills.set("_entityName", ReceiveBill.ENTITY_NAME);
                        receiveBills.set("voucherstatus", VoucherStatus.Empty.getValue());
                        cmpVoucherService.generateVoucherWithResult(receiveBills);
                    }
                    resultList.incSucessCount();
                    ctmcmpBusinessLogService.saveBusinessLog(receiveBill, receiveBill.getCode(), "", IServicecodeConstant.BANKRECONCILIATION, IMsgConstant.RECONCILIATE, IMsgConstant.AUTO_BILL);
                } catch (Exception e) {
                    //SqlHelper.rollback(status);
                    log.error(e.getMessage(), e);
                    resultList.incFailCount();
                    resultList.addMessage(e.getMessage());
                    BankReconciliation bankReconciliation = null;
                    for (Map.Entry<String, ReceiveBill> mm : map.entrySet()) {
                        if (mm.getValue() == receiveBill) {
                            for (BankReconciliation bankReconciliation1 : bankReconciliationList_tem) {
                                if (bankReconciliation1.getId().equals(mm.getKey())) {
                                    bankReconciliation = bankReconciliation1;
                                    break;
                                }
                            }
                            if (bankReconciliation != null) {
                                bankReconciliationList_tem.remove(bankReconciliation);
                                break;
                            }
                        }
                    }
                }

            }
        }
        //可生单单据为空，流程结束
        if (CollectionUtils.isEmpty(receiveBillList)) {
            return;
        }

        // 付款单 生成凭证 入库 登记日记账
        /*if (CollectionUtils.isNotEmpty(payBillList)) {
            fiarap4SettleAlready(payBillList.get(0),"AP");
            for (PayBill payBill : payBillList) {
                // 应付的单据 要判断是否已月结
                cmpVoucherService.generateVoucher(payBill);
                CmpMetaDaoHelper.insert(PayBill.ENTITY_NAME, payBill);

                Journal payBillJournal = createJournal(payBill, Direction.Credit);
                journalService.compute4Save(payBill, payBillJournal, Direction.Credit);
            }
        }*/

        // 生单成功后更新对账单状态 已生单
        for (BankReconciliation bank : bankReconciliationList_tem) {
            bank.setAutobill(true);//已生单
            bank.setCheckflag(true);//已勾兑
            EntityTool.setUpdateStatus(bank);
        }
        if (!bankReconciliationList_tem.isEmpty()) {
            CommonSaveUtils.updateBankReconciliation(bankReconciliationList_tem);
        }
    }

    /**
     * 处理银行对账单
     *
     * @param receiveBillList
     * @param payBillList
     * @param bankReconciliation
     * @throws Exception
     */
    private void createBill(List<ReceiveBill> receiveBillList, List<PayBill> payBillList,
                            BankReconciliation bankReconciliation, AutoConfig defaultConfig, int serviceAttr,Map<String, Object> defaultExchangeRateType,Long accBookTypeByAccBody,ResultList resultList,CtmJSONObject transTypeJson,Map<String, Object> periodMap) throws Exception {

        Map<String, String> paramMap = new HashMap<>();
        Long periodID = QueryBaseDocUtils.queryPeriodIdByAccbodyAndDate(bankReconciliation.getAccentity(),bankReconciliation.getTran_date());
        String transTypeId = getBillTypeId(paramMap, periodID,transTypeJson,periodMap);
        if (bankReconciliation.getDc_flag() != null && bankReconciliation.getDc_flag().equals(Direction.Debit)) {
            // 银行对账单的借 生成付款单
            payBillList.add(bankReconciliation2Payment(bankReconciliation, defaultConfig, transTypeId, serviceAttr, periodID,defaultExchangeRateType,accBookTypeByAccBody,resultList,periodMap));
        } else if (bankReconciliation.getDc_flag() != null
                && bankReconciliation.getDc_flag().equals(Direction.Credit)) {
            // 银行对账单的贷 生成收款单
            receiveBillList.add(bankReconciliation2Receive(bankReconciliation, defaultConfig, transTypeId, serviceAttr, periodID,defaultExchangeRateType,accBookTypeByAccBody,resultList,periodMap));
        }
    }

    /**
     * 应收应付的单据 判断是否已月结
     * @param bizObject
     * @param arap
     * @throws Exception
     */
    private void fiarap4SettleAlready(BizObject bizObject, String arap) throws Exception {
        if (bizObject.get("srcitem") != null && String.valueOf(EventSource.Manual.getValue()).equals(bizObject.get("srcitem").toString())) {
            if (Objects.equals("AR", arap)){
                if (periodaccentity.get(bizObject.get(IBussinessConstant.ACCENTITY).toString()+bizObject.get("period").toString())!=null
                        &&periodaccentity.get(bizObject.get(IBussinessConstant.ACCENTITY).toString()+bizObject.get("period").toString()).equals("1")){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100142"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180858","应收已月结，无法生成") /* "应收已月结，无法生成" */);
                }else if (periodaccentity.get(bizObject.get(IBussinessConstant.ACCENTITY).toString()+bizObject.get("period").toString())!=null
                        &&periodaccentity.get(bizObject.get(IBussinessConstant.ACCENTITY).toString()+bizObject.get("period").toString()).equals("0")){
                    return;
                }
            }else {
                if (periodaccentity_ap.get(bizObject.get(IBussinessConstant.ACCENTITY).toString()+bizObject.get("period").toString())!=null
                        &&periodaccentity_ap.get(bizObject.get(IBussinessConstant.ACCENTITY).toString()+bizObject.get("period").toString()).equals("1")){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100143"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180850","应付已月结，无法生成") /* "应付已月结，无法生成" */);
                }else if (periodaccentity_ap.get(bizObject.get(IBussinessConstant.ACCENTITY).toString()+bizObject.get("period").toString())!=null
                        &&periodaccentity_ap.get(bizObject.get(IBussinessConstant.ACCENTITY).toString()+bizObject.get("period").toString()).equals("0")){
                    return;
                }
            }

            Boolean settleAccountAlready =  RemoteDubbo.get(IArapSettleAccountService.class,IDomainConstant.MDD_DOMAIN_FIARAP).isSettleAccountAlready(bizObject);
            if (settleAccountAlready && Objects.equals("AR", arap)) {
                periodaccentity.put(bizObject.get(IBussinessConstant.ACCENTITY).toString()+bizObject.get("period").toString(),"1");
                periodaccentitys.add(bizObject.get(IBussinessConstant.ACCENTITY).toString()+bizObject.get("period").toString());
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100144"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0001038105") /* "应收已月结，无法生成" */);
            } else if (settleAccountAlready && Objects.equals("AP", arap)) {
                periodaccentity_ap.put(bizObject.get(IBussinessConstant.ACCENTITY).toString()+bizObject.get("period").toString(),"1");
                periodaccentitys_ap.add(bizObject.get(IBussinessConstant.ACCENTITY).toString()+bizObject.get("period").toString());
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100145"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0001038112") /* "应付已月结，无法生成" */);
            }

            if (Objects.equals("AR", arap)){
                periodaccentity.put(bizObject.get(IBussinessConstant.ACCENTITY).toString()+bizObject.get("period").toString(),"0");
                periodaccentitys.add(bizObject.get(IBussinessConstant.ACCENTITY).toString()+bizObject.get("period").toString());
            }else {
                periodaccentity_ap.put(bizObject.get(IBussinessConstant.ACCENTITY).toString()+bizObject.get("period").toString(),"0");
                periodaccentitys_ap.add(bizObject.get(IBussinessConstant.ACCENTITY).toString()+bizObject.get("period").toString());
            }

        }
    }

    /**
     * 生成收款单
     * @param bankReconciliation
     * @param defaultConfig
     * @param transTypeId
     * @param serviceAttr
     * @param periodID
     * @return
     * @throws Exception
     */
    private ReceiveBill bankReconciliation2Receive(BankReconciliation bankReconciliation,
                                                   AutoConfig defaultConfig, String transTypeId, int serviceAttr, Long periodID, Map<String, Object> defaultExchangeRateType, Long accBookTypeByAccBody, ResultList resultList,Map<String, Object> periodMap) throws Exception {
        ReceiveBill receiveBill = new ReceiveBill();
        receiveBill.setEntityStatus(EntityStatus.Insert);
        receiveBill.setAccentity(bankReconciliation.getAccentity());
        IBillCodeComponentService billCodeComponentService = AppContext.getBean(IBillCodeComponentService.class);
        String billcode = "";
        receiveBill.setPeriod(periodID);
        receiveBill.setBilltype(EventType.ReceiveBill);
        receiveBill.setSrctypeflag("auto");
        receiveBill.setInitflag(false);
        String currency = bankReconciliation.getCurrency();
        List<Map<String, Object>> stringObjectMap = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(bankReconciliation.getAccentity());
        String orgCurrency = null;
        if (!stringObjectMap.isEmpty()){
            receiveBill.setNatCurrency(stringObjectMap.get(0).get("currency").toString());
            orgCurrency = stringObjectMap.get(0).get("currency").toString();
        }
        receiveBill.setCurrency(currency);

        if (defaultExchangeRateType!=null&&defaultExchangeRateType.get("id")!=null) {
            receiveBill.setExchangeRateType(defaultExchangeRateType.get("id").toString());
        }
        if (!StringUtils.isEmpty(receiveBill.getAccentity())&&!StringUtils.isEmpty(receiveBill.getCurrency())){
            if (orgCurrency==null){
                orgCurrency = AccentityUtil.getNatCurrencyIdByAccentityId(receiveBill.getAccentity());
            }
            if (orgCurrency!=null){
                if (currency.equals(orgCurrency)){
                    receiveBill.setExchRate(BigDecimal.valueOf(1));
                }else {
                    if(bankReconciliation.getTran_date()==null) bankReconciliation.setTran_date(new Date());
                    try {
                        if (defaultExchangeRateType!=null&&defaultExchangeRateType.get("id")!=null){
                            receiveBill.setExchangeRateType(defaultExchangeRateType.get("id").toString());
                            Double currencyRateNew = CurrencyUtil.getCurrencyRateNew(null, defaultExchangeRateType.get("id").toString(),currency, orgCurrency,  bankReconciliation.getTran_date(), 6);
                            if (currencyRateNew==null||currencyRateNew==0.0){
                                throw IMultilangConstant.noRateError /* "未取到汇率" */;
                            }
                            receiveBill.setExchRate(BigDecimal.valueOf(currencyRateNew));
                        }
                    }catch (Exception e) {
                        log.error("查不到汇率类型，取不到汇率",e);
                        throw IMultilangConstant.noRateError/* "未取到汇率" */;
                    }
                }
            }

        }
        // 单据日期 取值 对账单的交易日期
        receiveBill.setVouchdate(bankReconciliation.getTran_date());
        receiveBill.setDzdate(bankReconciliation.getTran_date());
        if (serviceAttr == 0) {// 银行业务
            receiveBill.setEnterprisebankaccount(bankReconciliation.getBankaccount());
        } else if (serviceAttr == 1) {//
            receiveBill.setCashaccount(bankReconciliation.getBankaccount());
        }
        if(ValueUtils.isNotEmptyObj(bankReconciliation.getProject())){
            receiveBill.setProject(bankReconciliation.getProject());
        }
        receiveBill.setSettlemode(defaultConfig.getSettlemode());
        receiveBill.setTradetype(transTypeId);
        String toAcctName = bankReconciliation.getTo_acct_name();//账户名称
        String toAcctNo = bankReconciliation.getTo_acct_no();//账户
        // 收款单 先查客户 再查供应商
        customaccentitys.add(bankReconciliation.getAccentity()+ toAcctName+ toAcctNo);
        boolean cmpflag = false;//对方账户和账户名为空，则生成的单据为现金单据
        if(StringUtils.isEmpty(toAcctName) && StringUtils.isEmpty(toAcctNo)){
            cmpflag = true;
        }
        if(cmpflag){//生成现金单据
            receiveBill.setCaobject(CaObject.Other);
            receiveBill.setBilltype(EventType.ReceiveBill);
        }else if(customaccentity.get(bankReconciliation.getAccentity()+ toAcctName+ toAcctNo)!=null){//已查询到应收应付的单据生成对应单据
            if(!(customaccentity.get(bankReconciliation.getAccentity()+ toAcctName+ toAcctNo) instanceof CtmJSONObject)){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100146"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180851","当前单据无客商信息, 生单失败") /* "当前单据无客商信息, 生单失败" */);
            }
            CtmJSONObject obj = (CtmJSONObject) customaccentity.get(bankReconciliation.getAccentity()+ toAcctName+ toAcctNo);
            if (obj.get("customerId")==null&&obj.get("supplierId")!=null){
                receiveBill.setCaobject(CaObject.Supplier);
                receiveBill.setSupplier(obj.getLong("supplierId"));
                receiveBill.setSupplierbankaccount(obj.getLong("supplierbankId"));
                if (MapUtils.isNotEmpty(periodMap) && periodMap.get("begindate") != null
                        &&bankReconciliation.getTran_date().compareTo(DateUtils.strToDate(periodMap.get("begindate").toString()))>=0) {
                    receiveBill.setBilltype(EventType.ApRefund);
                }else{
                    receiveBill.setBilltype(EventType.ReceiveBill);
                }
            }else if (obj.get("customerId")!=null&&obj.get("supplierId")==null){
                receiveBill.setCaobject(CaObject.Customer);
                receiveBill.setCustomer(obj.getLong("customerId"));
                receiveBill.setCustomerbankaccount(obj.getLong("customerbankId"));
                receiveBill.setBilltype(EventType.ReceiveBill);
            }
        }else{//未查询的单子进行查询
            try {
                CtmJSONObject customerJson = getCustomer(bankReconciliation);
                if (Objects.equals(MerchantConstant.TRUE, customerJson.getString(MerchantConstant.CUSTOMERFLAG))) {
                    // 客户存在 生成客户的收款单
                    receiveBill.setCaobject(CaObject.Customer);
                    receiveBill.setCustomer(customerJson.getLong("customerId"));
                    receiveBill.setCustomerbankaccount(customerJson.getLong("customerbankId"));
                    receiveBill.setBilltype(EventType.ReceiveBill);
                    customaccentity.put(bankReconciliation.getAccentity()+ toAcctName+ toAcctNo,customerJson);
                } else {
                    CtmJSONObject supplierJson = getSupplier(bankReconciliation);
                    if (Objects.equals(MerchantConstant.TRUE, supplierJson.getString(MerchantConstant.VENDORFLAG))) {
                        // 供应商存在 生成供应商退款单
                        receiveBill.setCaobject(CaObject.Supplier);
                        receiveBill.setSupplier(supplierJson.getLong("supplierId"));
                        receiveBill.setSupplierbankaccount(supplierJson.getLong("supplierbankId"));
                        if (MapUtils.isNotEmpty(periodMap) && periodMap.get("begindate") != null
                                &&bankReconciliation.getTran_date().compareTo(DateUtils.strToDate(periodMap.get("begindate").toString()))>=0) {
                            receiveBill.setBilltype(EventType.ApRefund);
                        }else{
                            receiveBill.setBilltype(EventType.ReceiveBill);
                        }
                        customaccentity.put(bankReconciliation.getAccentity()+ toAcctName+ toAcctNo,supplierJson);
                    } else {
                        receiveBill.setCaobject(CaObject.Other);
                        receiveBill.setBilltype(EventType.ReceiveBill);
                        receiveBill.set("retailerAccountName", toAcctName);//付款账户名称
                        receiveBill.set("retailerAccountNo", toAcctNo);//付款账户名称
//                        receiveBill.set("retailerBankType",bankReconciliation.getTo_acct_bank());
//                        receiveBill.set("retailerBankType_name",bankReconciliation.getTo_acct_bank_name());
                        customaccentity.put(bankReconciliation.getAccentity()+ toAcctName+ toAcctNo,new HashMap<>());
//                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100147"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000153343") /* "当前单据无客商信息, 生单失败" */);
                    }
                }
            }catch (Exception e){
                customaccentity.put(bankReconciliation.getAccentity()+ toAcctName+ toAcctNo,false);//查询客户供应商信息失败
                throw e;
            }
        }

        if (MapUtils.isNotEmpty(periodMap) && periodMap.get("begindate") != null
                &&bankReconciliation.getTran_date().compareTo(DateUtils.strToDate(periodMap.get("begindate").toString()))>=0
                &&null != receiveBill.getCaobject() && !receiveBill.getCaobject().equals(CaObject.Other)) {
            // 应收会计期间 启用 则生成应收应付的单据
            receiveBill.setSrcitem(EventSource.Manual);
            receiveBill.setBasebilltype("FICA1");
            receiveBill.setBasebilltypecode("arap_receipt");
            receiveBill.setBookAmount(BigDecimal.ZERO);
            receiveBill.set("paytype", 2);
        } else {
            receiveBill.setSrcitem(EventSource.Cmpchase);
            receiveBill.setBilltype(EventType.CashMark);
            Map<String, Object> queryTransType = queryTransTypeById("FICM1");
            receiveBill.setTradetype(queryTransType.get("id").toString());
//            map.put("tradetype_id", queryTransType.get("id"));
//            map.put("tradetype_name", queryTransType.get("name"));
//            map.put("tradetype_code", queryTransType.get("code"));
        }

        if (receiveBill.getBilltype().equals(EventType.ApRefund)){
            Map<String, Object> condition = new HashMap<>();
            condition.put("code","arap_receipt_purchase");
            condition.put("billtype_id","FICA1");
            List<Map<String, Object>> transTypes = cmCommonService.getTransTypeByCondition(condition);
            if (!transTypes.isEmpty()){
                receiveBill.setTradetype(transTypes.get(0).get("id").toString());
            }
        }

        //receiveBill.setExchRate(new BigDecimal(1));
        receiveBill.setAuditstatus(AuditStatus.Complete);
        receiveBill.setSettlestatus(SettleStatus.alreadySettled);
        receiveBill.setStatus(Status.confirmed);
        receiveBill.setCmpflag(true);
        receiveBill.setVoucherstatus(VoucherStatus.Received);

        String cashierRobot = com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage(CASHIER_ROBOT);
        Date curDate = new Date();
        receiveBill.setDescription(bankReconciliation.getUse_name());
        receiveBill.setWriteoffstatus(WriteOffStatus.Incomplete);
        receiveBill.setAuditorId(AppContext.getCurrentUser().getId());
        // Auditor 不为空的时候不能删除
        //receiveBill.setAuditor(AppContext.getCurrentUser().getName());
        receiveBill.setAuditDate(curDate);
        receiveBill.setAuditTime(curDate);
        receiveBill.setSettleuser(cashierRobot);
        receiveBill.setSettledate(curDate);
        receiveBill.setCreator(AppContext.getCurrentUser().getName());
        receiveBill.put("creatorId",AppContext.getCurrentUser().getId());
        receiveBill.setCreateDate(curDate);
        receiveBill.setCreateTime(curDate);

        map.put(bankReconciliation.getId().toString(),receiveBill);


        // 收款单 取值 银行对账单的贷方金额
        BigDecimal creditamount = getMoneyDigit(currency, bankReconciliation.getCreditamount());
        receiveBill.setOriSum(creditamount);
        if(receiveBill.getExchRate()!=null){
            receiveBill.setNatSum(receiveBill.getOriSum().multiply(receiveBill.getExchRate()));
        }

        receiveBill.setBalance(receiveBill.getOriSum());
        receiveBill.setLocalbalance(receiveBill.getNatSum());

        if (MapUtils.isNotEmpty(periodMap) && periodMap.get("begindate") != null
                &&bankReconciliation.getTran_date().compareTo(DateUtils.strToDate(periodMap.get("begindate").toString()))>=0
                &&null != receiveBill.getCaobject() && !receiveBill.getCaobject().equals(CaObject.Other)) {
            // 应收会计期间 启用 则生成应收应付的单据
            billcode= billCodeComponentService.getBillCode(CmpBillCodeMappingConfUtils.getBillCode("arap_receivebill"),
                    ReceiveBill.ENTITY_NAME, InvocationInfoProxy.getTenantid(), "",
                    new BillCodeObj(receiveBill), true);
        } else {
            billcode= billCodeComponentService.getBillCode(CmpBillCodeMappingConfUtils.getBillCode(IBillNumConstant.RECEIVE_BILL),
                    ReceiveBill.ENTITY_NAME, InvocationInfoProxy.getTenantid(), "",
                    new BillCodeObj(receiveBill), true);
        }
        receiveBill.setCode(billcode);
        receiveBill.setId(ymsOidGenerator.nextId());
        receiveBill.set("transeqno", bankReconciliation.getBank_seq_no());
        receiveBill.setVerifystate(VerifyState.INIT_NEW_OPEN.getValue());// 自动生单单据审批流状态初始值为Verifystate
        ReceiveBill_b subBill = new ReceiveBill_b();
        subBill.setOriSum(creditamount);
        if (receiveBill.getExchRate()!=null){
            subBill.setNatSum(receiveBill.getOriSum().multiply(receiveBill.getExchRate()));
        }
        if(ValueUtils.isNotEmptyObj(bankReconciliation.getProject())){
            subBill.setProject(bankReconciliation.getProject());
        }
        subBill.setBookAmount(BigDecimal.ZERO);
        subBill.setNatSum(receiveBill.getOriSum().multiply(receiveBill.getExchRate()));
        subBill.setBalance(receiveBill.getOriSum());
        subBill.setLocalbalance(receiveBill.getNatSum());
        subBill.setMainid(receiveBill.getId());
        subBill.setId(ymsOidGenerator.nextId());
        subBill.setDescription(bankReconciliation.getUse_name());
        subBill.setQuickType(defaultConfig.getReceiveQuickType());
        if (CaObject.Customer.getValue() == receiveBill.getCaobject().getValue()) {
            subBill.setCustomer(receiveBill.getCustomer());
        } else if (CaObject.Supplier.getValue() == receiveBill.getCaobject().getValue()) {
            subBill.setSupplier(receiveBill.getSupplier());
            Map<String, Object> condition  = new HashMap<String, Object>();
            condition.put("id",defaultConfig.getReceiveQuickType());
            if (QueryBaseDocUtils.queryQuickTypeByCondition(condition).get(0).get("name").toString().equals(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418085C","预收款") /* "预收款" */)) {
                condition  = new HashMap<String, Object>();
                condition.put("name", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418085E","预付款") /* "预付款" */);
                subBill.setQuickType((Long) QueryBaseDocUtils.queryQuickTypeByCondition(condition).get(0).get("id"));
            } else if (QueryBaseDocUtils.queryQuickTypeByCondition(condition).get(0).get("name").toString().equals(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418085F","应收款") /* "应收款" */)) {
                condition  = new HashMap<String, Object>();
                condition.put("name", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180860","应付款") /* "应付款" */);
                subBill.setQuickType((Long) QueryBaseDocUtils.queryQuickTypeByCondition(condition).get(0).get("id"));
            }
        } else{
            // 其他的场景下处理逻辑
        }
        subBill.setEntityStatus(EntityStatus.Insert);
        List<ReceiveBill_b> subBillList = new ArrayList<>();
        subBillList.add(subBill);
        receiveBill.setReceiveBill_b(subBillList);
        return receiveBill;
    }

    /**
     * 获取对应币种精度的金额
     *
     * @param currency
     * @param amount
     * @return
     */
    private BigDecimal getMoneyDigit(String currency, BigDecimal amount) throws Exception {
        CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(currency);
        if (currencyDTO == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100148"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180868","当前币种不存在") /* "当前币种不存在" */);
        }
        amount = amount.setScale(currencyDTO.getMoneydigit(), RoundingMode.HALF_UP);
        return amount;
    }


    /**
     * 生成付款单
     * @param bankReconciliation
     * @param defaultConfig
     * @param transTypeId
     * @param serviceAttr
     * @param periodID
     * @return
     * @throws Exception
     */
    private PayBill bankReconciliation2Payment(BankReconciliation bankReconciliation, AutoConfig defaultConfig,
                                               String transTypeId, int serviceAttr, Long periodID,Map<String, Object> defaultExchangeRateType,Long accBookTypeByAccBody,ResultList resultList,Map<String, Object> periodMap) throws Exception {
        PayBill payBill = new PayBill();
        payBill.setAccentity(bankReconciliation.getAccentity());

        if (periodID != null&&periodID>0) {
            // 应付会计期间 启用 则生成应收应付的单据
            payBill.setSrcitem(EventSource.Manual);
            payBill.setPeriod(periodID);
        } else {
            payBill.setSrcitem(EventSource.Cmpchase);
        }
        payBill.setSrctypeflag(ICmpConstant.AUTO_BILL_FLAG);

        String currency = bankReconciliation.getCurrency();
        payBill.setNatCurrency(currency);
        payBill.setCurrency(currency);
        if (defaultExchangeRateType!=null&&defaultExchangeRateType.get("id")!=null) {
            payBill.setExchangeRateType(defaultExchangeRateType.get("id").toString());
        }
        if (!StringUtils.isEmpty(payBill.getAccentity())&&!StringUtils.isEmpty(payBill.getCurrency())){
            String orgCurrency = AccentityUtil.getNatCurrencyIdByAccentityId(payBill.getAccentity());
            if (orgCurrency!=null){
                if (currency.equals(orgCurrency)){
                    payBill.setExchRate(BigDecimal.valueOf(1));
                }else {
                    if(bankReconciliation.getTran_date()==null) bankReconciliation.setTran_date(new Date());
                    if (defaultExchangeRateType!=null&&defaultExchangeRateType.get("id")!=null){
                        try {
                            Double currencyRateNew = CurrencyUtil.getCurrencyRateNew(null, defaultExchangeRateType.get("id").toString(),currency, orgCurrency,  bankReconciliation.getTran_date(), 6);
                            if (currencyRateNew==null|| currencyRateNew==0){
                                throw IMultilangConstant.noRateError/*未取到汇率 */;
                            }
                            payBill.setExchRate(BigDecimal.valueOf(currencyRateNew));
                        }catch (Exception e) {
                            log.error("取不到汇率",e);
                            throw IMultilangConstant.noRateError/*未取到汇率 */;
                        }
                    }
                }
            }
        }
        // 单据日期 取值对账单的交易日期
        payBill.setVouchdate(bankReconciliation.getTran_date());
        if (serviceAttr == 0) {// 银行业务
            payBill.setEnterprisebankaccount(bankReconciliation.getBankaccount());
        } else if (serviceAttr == 1) {// 现金业务
            payBill.setCashaccount(bankReconciliation.getBankaccount());
        }
        payBill.setSettlemode(defaultConfig.getSettlemode());
        payBill.setTradetype(transTypeId);
        payBill.setCmpflag(true);
        // 付款单 先供应商  后客户
        CtmJSONObject supplierJson = getSupplier(bankReconciliation);
        if (Objects.equals(MerchantConstant.TRUE, supplierJson.getString(MerchantConstant.VENDORFLAG))) {
            // 供应商存在 生成供应商付款单
            payBill.setCaobject(CaObject.Supplier);
            payBill.setSupplier(supplierJson.getLong("supplierId"));
            payBill.setSupplierbankaccount(supplierJson.getLong("supplierbankId"));
            payBill.setBilltype(EventType.PayMent);
        } else {
            CtmJSONObject customerJson = getCustomer(bankReconciliation);
            if (Objects.equals(MerchantConstant.TRUE, customerJson.getString(MerchantConstant.CUSTOMERFLAG))) {
                // 客户存在 生成客户的退款单
                payBill.setCaobject(CaObject.Customer);
                payBill.setCustomer(customerJson.getLong("customerId"));
                payBill.setCustomerbankaccount(customerJson.getLong("customerbankId"));
                payBill.setBilltype(EventType.ArRefund);
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100149"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0025A", "当前单据无客商信息, 生单失败") /* "当前单据无客商信息, 生单失败" */);
            }
        }
        //payBill.setExchRate(new BigDecimal(1));
        payBill.setAuditstatus(AuditStatus.Complete);
        payBill.setSettlestatus(SettleStatus.alreadySettled);
        payBill.setPaystatus(PayStatus.Success);
        payBill.setVoucherstatus(VoucherStatus.Empty);
        payBill.setStatus(Status.confirmed);

        String cashierRobot = com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage(CASHIER_ROBOT);
        Date curDate = new Date();
        payBill.setDescription(bankReconciliation.getUse_name());
        payBill.setWriteoffstatus(WriteOffStatus.Incomplete);
        payBill.setAuditor(cashierRobot);
        payBill.setAuditDate(curDate);
        payBill.setAuditTime(curDate);
        payBill.setSettleuser(cashierRobot);
        payBill.setSettledate(curDate);
        payBill.setCreator(cashierRobot);
        payBill.setCreateDate(curDate);
        payBill.setCreateTime(curDate);

        // 付款单 取值对账单的借方金额
        BigDecimal debitamount = getMoneyDigit(currency, bankReconciliation.getDebitamount());
        payBill.setOriSum(debitamount);
        if (payBill.getExchRate()!=null){
            payBill.setNatSum(payBill.getOriSum().multiply(payBill.getExchRate()));
        }
        payBill.setBalance(payBill.getOriSum());
        payBill.setLocalbalance(payBill.getNatSum());
        IBillCodeComponentService billCodeComponentService = AppContext.getBean(IBillCodeComponentService.class);
        String billcode = billCodeComponentService.getBillCode(CmpBillCodeMappingConfUtils.getBillCode("cmp_payment"),
                ReceiveBill.ENTITY_NAME, InvocationInfoProxy.getTenantid(), "",
                new BillCodeObj(payBill), true);
        payBill.setCode(billcode);
        payBill.setId(ymsOidGenerator.nextId());

        PayBillb subBill = new PayBillb();
        subBill.setOriSum(debitamount);
        if (payBill.getExchRate()!=null){
            subBill.setNatSum(payBill.getOriSum().multiply(payBill.getExchRate()));
        }
        subBill.setBalance(payBill.getOriSum());
        subBill.setLocalbalance(payBill.getNatSum());
        subBill.setQuickType(defaultConfig.getPayQuickType());
        subBill.setMainid(payBill.getId());
        subBill.setId(ymsOidGenerator.nextId());
        subBill.setDescription(bankReconciliation.getUse_name());
        if (CaObject.Customer == payBill.getCaobject()) {
            subBill.setCustomer(payBill.getCustomer());
        } else {
            subBill.setSupplier(payBill.getSupplier());
        }

        List<PayBillb> subBillList = new ArrayList<>();
        subBillList.add(subBill);
        payBill.setPayBillb(subBillList);
        return payBill;
    }


    /**
     * 获取会计期间信息
     *
     * @param accentity
     * @param module
     * @return
     * @throws Exception
     */
    private Map<String, Object> getPeriod(String accentity, String module) throws Exception {
        //判断是否启用 收付  启用则生成单据类型为 应收应付  没启用则为 现金管理
        Date begindate = QueryBaseDocUtils.queryOrgPeriodBeginDate(accentity, ISystemCodeConstant.ORG_MODULE_AR);
        Map<String, Object> returnMap = new HashMap<>();
        returnMap.put("begindate",begindate);
        return returnMap;
    }

    /**
     * 查询交易类型  应收应付-arap_receipt_sale   现金管理-default
     *
     * @param paramMap
     * @param transTypeJson
     * @return
     */
    private String getBillTypeId(Map<String, String> paramMap, Long periodId,CtmJSONObject transTypeJson,Map<String, Object> periodMap) {
        boolean flag = false;
        if (MapUtils.isNotEmpty(periodMap) && periodMap.get("begindate") != null) {
            // 应收会计期间 启用 则生成应收应付
            flag = true;
        }
        String transTypeId = null;
        CtmJSONArray dataArr = transTypeJson.getJSONArray("data");
        for (int i = 0; i < dataArr.size(); i++) {
            CtmJSONObject typeObj = dataArr.getJSONObject(i);
            if (flag && Objects.equals(ARAP_RECEIPT_SALE, typeObj.getString("code"))) {
                transTypeId = typeObj.getString("id");
                break;
            }
            if (!flag && Objects.equals("1", typeObj.getString("default"))) {
                transTypeId = typeObj.getString("id");
                break;
            }
        }
        return transTypeId;
    }

    /**
     * 查询结算方式业务属性
     *
     * @param autoConfig
     * @return
     * @throws Exception
     */
    private Integer getServiceAttr(AutoConfig autoConfig) throws Exception {
        SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
        settleMethodQueryParam.setId(autoConfig.getSettlemode());
        settleMethodQueryParam.setIsEnabled(1);
        settleMethodQueryParam.setTenantId(AppContext.getTenantId());
        List<SettleMethodModel> dataList = baseRefRpcService.querySettleMethods(settleMethodQueryParam);
        Integer serviceAttr = null;
        if (dataList != null && dataList.size() > 0) {
            serviceAttr = dataList.get(0).getServiceAttr();
        }
        return serviceAttr;
    }

    /**
     * 查询客户
     *
     * @param bankReconciliation
     * @return
     */
    private static CtmJSONObject getCustomer(BankReconciliation bankReconciliation) throws Exception {
        CtmJSONObject resultObj = new CtmJSONObject();
        // 客户判断
        resultObj.put(MerchantConstant.CUSTOMERFLAG, MerchantConstant.FALSE);
        String accentity = bankReconciliation.getAccentity();
        if (StringUtils.isEmpty(bankReconciliation.getTo_acct_name())||StringUtils.isEmpty(bankReconciliation.getTo_acct_no())) {
            return resultObj;
            //throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100150"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0001008424") /* "客商查询该对账单对方户名不能为空" */);
        }
        if (StringUtils.isEmpty(bankReconciliation.getTo_acct_no())) {
            return resultObj;
            //throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100151"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0001008435") /* "客商查询该对账单对方账号不能为空" */);
        }
        MerchantRequst requst = new MerchantRequst(accentity, bankReconciliation.getTo_acct_name(), bankReconciliation.getTo_acct_no());
        CtmJSONObject cust2Check = MerchantUtils.cust2Check(requst);
        if (Objects.equals(MerchantConstant.TRUE, cust2Check.getString(MerchantConstant.CUSTOMERFLAG))) {
            if (Objects.equals(true, cust2Check.getBoolean(MerchantConstant.STOPSTATUS))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100152"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0001008426") /* "当前客户档案已停用" */);
            }
            // 客户名称和账号都存在
            resultObj.put("type", CaObject.Customer.getValue());
            resultObj.put("customerId", cust2Check.getLong(MerchantConstant.CUSTOMERID));
            resultObj.put("customerbankId", cust2Check.getLong(MerchantConstant.CUSTOMERBANKID));
            resultObj.put(MerchantConstant.CUSTOMERFLAG, MerchantConstant.TRUE);
            return resultObj;
        }

        return resultObj;
        /*
        if (Objects.equals(MerchantConstant.TRUE, cust2Check.getString(MerchantConstant.CUSTOMERFLAG))) {
            // 客户名称存在 账号不存在 则只需要同步银行账号
            CtmJSONObject customerJson = MerchantUtils.getDefaultCutomer(bankReconciliation);
            MerchantResult newCustomer = MerchantUtils.saveCust(customerJson);
            if (newCustomer.getCode() != 200) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100153"),newCustomer.getMessage());
            }
            CtmJSONObject newCustomerJson = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(newCustomer.getData()));
            CtmJSONArray infoArray = newCustomerJson.getJSONArray("infos");
            resultObj.put("type", CaObject.Customer.getValue());
            resultObj.put("customerId", infoArray.getJSONObject(0).getLong("id"));
            resultObj.put("customerbankId", infoArray.getJSONObject(0).getJSONArray("merchantAgentFinancialInfos")
                    .getJSONObject(0).getLong("id"));
            return resultObj;
        }*/
        // 供应商判断
        /*CtmJSONObject vendor2Check = MerchantUtils.vendor2Check(accentity, bankReconciliation.getTo_acct_name(),
                bankReconciliation.getTo_acct_no());
        if (Objects.equals(MerchantConstant.TRUE, vendor2Check.getString(MerchantConstant.VENDORFLAG))) {
            if (Objects.equals(true, vendor2Check.getBoolean(MerchantConstant.STOPSTATUS))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100154"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0001008432") *//* "当前供应商档案已停用" *//*);
            }
            // 供应商名称和账号都存在
            resultObj.put("type", CaObject.Supplier.getValue());
            resultObj.put("supplierId", vendor2Check.getLong(MerchantConstant.VENDORID));
            resultObj.put("supplierbankId", vendor2Check.getLong(MerchantConstant.VENDORBANKID));
            return resultObj;
        }
        if (Objects.equals(MerchantConstant.FALSE, vendor2Check.getString(MerchantConstant.VENDORFLAG))) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100155"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000153343") *//* "当前单据无客商信息, 生单失败" *//*);
        }*/
        // 客户和供应商 （名称和银行账号匹配失败 ） 则新增为客商信息
        /*CtmJSONObject customerJson = MerchantUtils.getDefaultCutomer(bankReconciliation);
        MerchantResult newCustomer = MerchantUtils.saveCust(customerJson);
        if (newCustomer.getCode() != 200) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100156"),newCustomer.getMessage());
        }
        CtmJSONObject newCustomerJson = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(newCustomer.getData()));
        CtmJSONArray infoArray = newCustomerJson.getJSONArray("infos");
        resultObj.put("type", CaObject.Customer.getValue());
        resultObj.put("customerId", infoArray.getJSONObject(0).getLong("id"));
        resultObj.put("customerbankId",
                infoArray.getJSONObject(0).getJSONArray("merchantAgentFinancialInfos").getJSONObject(0).getLong("id"));
        */

    }

    /**
     * 查询供应商
     *
     * @param bankReconciliation
     * @return
     */
    private static CtmJSONObject getSupplier(BankReconciliation bankReconciliation) throws Exception {
        CtmJSONObject resultObj = new CtmJSONObject();
        // 客户判断
        String accentity = bankReconciliation.getAccentity();
        resultObj.put(MerchantConstant.VENDORFLAG, MerchantConstant.FALSE);
        if (StringUtils.isEmpty(bankReconciliation.getTo_acct_name())||StringUtils.isEmpty(bankReconciliation.getTo_acct_no())) {
            return resultObj;
            //throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100157"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0001008431") /* "供应商查询该对账单对方户名不能为空" */);
        }
        if (StringUtils.isEmpty(bankReconciliation.getTo_acct_no())) {
            return resultObj;
            //throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100158"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0001008421") /* "供应商查询该对账单对方账号不能为空" */);
        }
        // 供应商判断
        MerchantRequst requst = new MerchantRequst(accentity, bankReconciliation.getTo_acct_name(), bankReconciliation.getTo_acct_no());
        CtmJSONObject vendor2Check = MerchantUtils.vendor2Check(requst);
        if (Objects.equals(MerchantConstant.TRUE, vendor2Check.getString(MerchantConstant.VENDORFLAG))) {
            if (Objects.equals(true, vendor2Check.getBoolean(MerchantConstant.STOPSTATUS))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100159"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0001008432") /* "当前供应商档案已停用" */);
            }
            // 供应商名称和账号都存在
            resultObj.put("type", CaObject.Supplier.getValue());
            resultObj.put("supplierId", vendor2Check.getLong(MerchantConstant.VENDORID));
            resultObj.put("supplierbankId", vendor2Check.getLong(MerchantConstant.VENDORBANKID));
            resultObj.put(MerchantConstant.VENDORFLAG, MerchantConstant.TRUE);
            return resultObj;
        }
        if (Objects.equals(MerchantConstant.FALSE, vendor2Check.getString(MerchantConstant.VENDORFLAG))) {
            // throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100160"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0001008434") /* "当前单据无供应商信息,生单失败" */);
            resultObj.put(MerchantConstant.VENDORFLAG, MerchantConstant.FALSE);
        }
        return resultObj;
    }

    /**
     * 登记日记账
     * @param bizObject
     * @param direction
     * @return
     * @throws Exception
     */
    private Journal createJournal(BizObject bizObject, Direction direction) throws Exception {
        Journal journal = new Journal();
        journal.set(IBussinessConstant.ACCENTITY, bizObject.get(IBussinessConstant.ACCENTITY));
        journal.set("period", bizObject.get("period"));
        journal.set("bankaccount", bizObject.get("enterprisebankaccount"));
        EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(bizObject.get("enterprisebankaccount"));
        if(enterpriseBankAcctVO != null ){
            journal.setBankaccountno(enterpriseBankAcctVO.getAccount());
            journal.setBanktype(enterpriseBankAcctVO.getBank());
        }
        journal.set("cashaccount", bizObject.get("cashaccount"));
        journal.set("cashaccountno", "");
        journal.set("currency", bizObject.get("currency"));
        if (Direction.Debit.equals(direction)) {
            journal.set("direction", Direction.Debit.getValue());
            journal.set("debitoriSum", bizObject.get(IBussinessConstant.ORI_SUM));
            journal.set("debitnatSum", bizObject.get(IBussinessConstant.NAT_SUM));
            journal.set("creditoriSum", BigDecimal.ZERO);
            journal.set("creditnatSum", BigDecimal.ZERO);
            journal.set("rptype", RpType.ReceiveBill.getValue());
        }
        if (Direction.Credit.equals(direction)) {
            journal.set("direction", Direction.Credit.getValue());
            journal.set("debitoriSum", BigDecimal.ZERO);
            journal.set("debitnatSum", BigDecimal.ZERO);
            journal.set("creditoriSum", bizObject.get(IBussinessConstant.ORI_SUM));
            journal.set("creditnatSum", bizObject.get(IBussinessConstant.NAT_SUM));
            journal.set("rptype", RpType.PayBill.getValue());
        }
        // journal.set("datacontent","");
        Date vouchdate = bizObject.get("vouchdate");
        journal.set("vouchdate", vouchdate);
        /*QueryConditionGroup condition = new QueryConditionGroup();
        if (journal.getBankaccount() != null) {
            condition.addCondition(
                    QueryConditionGroup.and(QueryCondition.name("bankaccount").eq(journal.getBankaccount())));
        }
        if (journal.getCashaccount() != null) {
            condition.addCondition(
                    QueryConditionGroup.and(QueryCondition.name("cashaccount").eq(journal.getCashaccount())));
        }
        condition.addCondition(QueryConditionGroup.and(QueryCondition.name("dzdate").gt(vouchdate)));
        QuerySchema querySchema = QuerySchema.create().addSelect("1");
        querySchema.addCondition(condition);
        List<Map<String, Object>> journalAfterList = MetaDaoHelper.query(Journal.ENTITY_NAME, querySchema);
        if (journalAfterList != null && journalAfterList.size() > 0) {
            journal.set("dzdate", vouchdate);
        } else {
            journal.set("dzdate", new Date());
        }*/
        journal.set("dzdate", vouchdate);
        journal.set("description", bizObject.get("description"));
        journal.set("srcitem", bizObject.get("srcitem"));
        journal.set("billtype", bizObject.get("billtype"));
        journal.set("topsrcitem", bizObject.get("srcitem"));
        journal.set("topbilltype", bizObject.get("billtype"));
        journal.set("tradetype", bizObject.get("tradetype"));
        journal.set("exchangerate", bizObject.get("exchRate"));
        journal.set("settlemode", bizObject.get("settlemode"));
        journal.set("oribalance", bizObject.get(IBussinessConstant.ORI_SUM));
        journal.set("natbalance", bizObject.get(IBussinessConstant.NAT_SUM));
        journal.set("noteno", bizObject.get("noteno"));
        journal.set("bankbilltype", "");
        // journal.set("bankbilldate","");
        journal.set("customerbankaccount", bizObject.get("customerbankaccount"));
        journal.set("customerbankname", bizObject.get("customerbankname"));
        journal.set("supplierbankaccount", bizObject.get("supplierbankaccount"));
        journal.set("supplierbankname", bizObject.get("supplierbankname"));
        journal.set("employeeaccount", bizObject.get("staffBankAccount"));
        journal.set("caobject", bizObject.get("caobject"));
        journal.set("customer", bizObject.get("customer"));
        journal.set("supplier", bizObject.get("supplier"));
        journal.set("employee", bizObject.get("employee"));
        journal.set("dept", bizObject.get("dept"));
        journal.set("checkflag", true);
        journal.set("insidecheckflag", false);
        if (bizObject.get("paystatus") != null) {
            journal.set("paymentstatus", bizObject.get("paystatus"));
        } else {
            journal.set("paymentstatus", PaymentStatus.NoPay.getValue());
        }
        journal.set("auditstatus", bizObject.get("auditstatus"));
        journal.set("settlestatus", bizObject.get("settlestatus"));
        journal.set("project", bizObject.get("project"));
        journal.set("costproject", bizObject.get("expenseitem"));
        journal.set("refund", false);
        journal.set("bookkeeper", AppContext.getCurrentUser().getId());
        journal.set("auditinformation", "");
        journal.set("srcbillno", bizObject.get("code"));
        journal.set("srcbillitemid", bizObject.get("id"));
        // 生成应收应付单据，日记账添加来源单据号，来源单据id
        if (bizObject.get("srcitem").toString().equals(String.valueOf(EventSource.Manual.getValue()))){
            journal.set("topsrcbillno", bizObject.get("code"));
            journal.set("topsrcbillitemid", bizObject.get("id"));
        }
        journal.set("srcbillitemno", BigDecimal.ONE);
        journal.set("org", bizObject.get("org"));
        journal.set("reconciliation", "");
        journal.set("billnum", bizObject.get("code"));
        journal.set("createTime", new Date());
        journal.set("createDate", new Date());
        journal.set("creator", AppContext.getCurrentUser().getId());
        journal.set("corp", bizObject.get("corp"));
        journal.set("financialOrg", bizObject.get("financialOrg"));
        journal.set("tenant", bizObject.get("tenant"));
        journal.set("transeqno", bizObject.get("transeqno"));
        return journal;
    }
    /*
     *@Author tongyd
     *@Description 根据CODE查询交易类型
     *@Date 2019/6/27 20:59
     *@Param [id]
     *@Return java.util.Map<java.lang.String,java.lang.Object>
     **/
    public static Map<String, Object> queryTransTypeById(String billtype) throws Exception {
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.bill.TransType");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_TRANSTYPE);
        QuerySchema schema = QuerySchema.create();
        schema.addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("billtype_id").eq(billtype));
        conditionGroup.appendCondition(QueryCondition.name("default").eq("1"));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> transtype = MetaDaoHelper.query(billContext, schema);
        if(transtype.size()<1){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100161"),com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_CTM_CM-BE_0001566084"));
        }
        return transtype.get(0);
    }
}
