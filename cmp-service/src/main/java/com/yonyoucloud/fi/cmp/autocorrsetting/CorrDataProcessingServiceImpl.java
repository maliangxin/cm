package com.yonyoucloud.fi.cmp.autocorrsetting;


import com.yonyou.cloud.utils.StringUtils;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.base.tenant.Tenant;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyoucloud.ctm.stwb.reconcode.pubitf.ReconciliateCodeGenerator;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationService;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmErrorCode;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.ctm.stwb.api.settlebench.SettleBenchBRPCService;
import com.yonyoucloud.ctm.stwb.reqvo.QuerySettleDeatailReqVO;
import com.yonyoucloud.ctm.stwb.respvo.ResultSettleDetailRespVO;
import com.yonyoucloud.ctm.stwb.respvo.SettleDetailRespVO;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.autocorrsettings.Autocorrsetting;
import com.yonyoucloud.fi.cmp.autocorrsettings.BussDocumentType;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankRecRelationCharacterDef;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.enums.BankReconciliationActions;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.CorrDataEntity;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.TaskUtils;
import com.yonyoucloud.fi.cmp.util.threadpool.ThreadPoolBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.imeta.biz.base.Objectlizer;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

;

/**
 * 关联数据处理
 * 现金管理 -->资金结算
 * 银行对账单匹配业务单据
 *
 * @author msc
 */
@Service
@Slf4j
public class CorrDataProcessingServiceImpl implements CorrDataProcessingService {


    private final String DATA_TYPE_DOUBLE = "Double";
    private final String DATA_TYPE_FLOAT = "Float";
    private final String DATA_TYPE_BIGDECIMAL = "BigDecimal";
    private final String DATA_TYPE_INTEGER = "Integer";
    private final String DATA_TYPE_LONG = "Long";
    private final String DATA_TYPE_SHORT = "Short";
    private final String AUTOCORRSETTINGLISTMAPPER = "com.yonyoucloud.fi.cmp.mapper.AutoCorrSettingMapper.";
    public static final String fundsettlement = "stwb_settlebench";//资金结算明细--billNum

    @Autowired
    private CtmThreadPoolExecutor executorServicePool;

    @Autowired
    YmsOidGenerator ymsOidGenerator;

    @Autowired
    AutoCorrServiceImpl autoCorrService;


    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;

    @Autowired
    private AutoConfigService autoConfigService;
    @Autowired
    BankreconciliationService bankreconciliationService;

    /**
     * 自动关联数据匹配
     *
     * @throws Exception
     */
    @Override
    @Transactional
    public List<CorrDataEntity> autoAssociatedData(List<BankReconciliation> bankReconciliationList) throws Exception {
        /**
         * 1.查询业务单据，判断是否可以关联
         * 2.将未关联的数据设置为已执行自动关联
         */
        ExecutorService autoAssociatedDataExecutor = null;
        Vector<CorrDataEntity> resList = new Vector<>();
        try {
            //todo 这里为啥还要再查一次
//            List<BankReconciliation> list = getBankReconciliationList(dataRange);
            Vector<BankReconciliation> noAssoDataList = new Vector();
            if (bankReconciliationList != null && bankReconciliationList.size() > 0) {
                //租户级自动关联规则 - 贷的规则
                List<Autocorrsetting> acListCredit = getAutoCorrSettingCredit(bankReconciliationList.get(0));
                //租户级自动关联规则 - 借的规则
                List<Autocorrsetting> acListDebit = getAutoCorrSettingDebit(bankReconciliationList.get(0));
                try {
                    //租户级自动关联规则 - 借的规则
                    //适配自动任务进行关联操作
                    autoAssociatedDataExecutor = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(10, 10, 10000, "detailRelationSettle");
                    ExecutorService finalAutoAssociatedDataExecutor = autoAssociatedDataExecutor;
                    List<? extends Future<?>> futures = bankReconciliationList.stream().map(bankReconciliation -> {
                        return finalAutoAssociatedDataExecutor.submit(() -> {
                            // 这里只是查询数据，不涉及数据库操作
                            ergodicData(bankReconciliation, acListCredit, acListDebit, resList, noAssoDataList);
                        });
                    }).collect(Collectors.toList());
                    futures.forEach(future -> {
                        try {
                            future.get();
                        } catch (Exception e) {
                            log.error("自动关联失败！，失败原因：", e);
                        }
                    });
                    log.error("自动关联数据匹配！已关联数据量：" + resList.size());
                    log.error("自动关联数据匹配！未关联数据量：" + noAssoDataList.size());
                    // 以下单起线程更新未命中的流水自动关联任务标识
                    if (CollectionUtils.isNotEmpty(noAssoDataList)) {
                        autoAssociatedDataExecutor.submit(() -> {
                            try {
                                CommonSaveUtils.repairBankReconciliationSatus(noAssoDataList);
                                autoCorrService.updateBankRecAutoassociation(noAssoDataList);
                            } catch (Exception e) {
                                log.error("更新自动关联任务标识失败！失败原因：", e);
                            }
                        });
                    }
                } catch (Exception e) {
                    log.error("执行自动关联数据匹配任务失败", e);
                    throw e;
                } finally {
                    if (autoAssociatedDataExecutor != null) {
                        autoAssociatedDataExecutor.shutdown();
                    }
                }
            }
        } finally {
            if (autoAssociatedDataExecutor != null) {
                autoAssociatedDataExecutor.shutdown();
            }
        }
        return resList;
    }

    /**
     * 执行匹配需要关联的数据
     * 组装数据可以不往外抛错，不存在数据更新，可以不抛错
     *
     * @param bankReconciliation
     * @param acListCredit
     * @param acListDebit
     * @param resList
     * @param noAssoDataList
     */
    private void ergodicData(BankReconciliation bankReconciliation, List<Autocorrsetting> acListCredit, List<Autocorrsetting> acListDebit, Vector<CorrDataEntity> resList, Vector<BankReconciliation> noAssoDataList) {
        boolean noAssoDataFlag = true;
        try {
            //循环匹配对账单
            /**
             * 负数生单需求优化 如果金额为负数，匹配规则取相反方向，金额取绝对值
             */
            List<Autocorrsetting> acList = bankReconciliation.getDc_flag().getValue() == 1 ? acListDebit : acListCredit;
            if (BigDecimal.ZERO.compareTo(bankReconciliation.getTran_amt()) > 0) {
                acList = bankReconciliation.getDc_flag().getValue() == 1 ? acListCredit : acListDebit;
            }
            //查询现金参数 转账单是否传结算
            Boolean checkFundTransfer = autoConfigService.getCheckFundTransfer();
            /**
             * 根据单个对账单的自动关联设置，循环匹配业务单据
             */
            for (int j = 0; j < acList.size(); j++) {
                Autocorrsetting ac = acList.get(j);
                Map<String,Object> param = selectData(ac, bankReconciliation);//获取拼接匹配业务单据的参数
                //关联资金结算单
                if (BussDocumentType.settlebench.getValue() == ac.getBusDocumentType()) {
                    QuerySettleDeatailReqVO json = new QuerySettleDeatailReqVO();
                    json.setBankcheckno(bankReconciliation.getBankcheckno());
                    json.setBankaccount(bankReconciliation.getBankaccount());
                    // CZFW-481860 支持去掉账号、名称强制二选一，都可以为空
                    // 0 无  1 相同
                    // 对方银行账号
                    if (ac.getOthBankNum() == 1) {
                        json.setTo_acct_no(bankReconciliation.getTo_acct_no());
                        json.setIsMatchCounterNo("1");
                    } else {
                        json.setIsMatchCounterNo("0");
                    }
                    // 对方户名
                    if (ac.getOthBankNumName() == 1) {
                        json.setOthbanknumname(bankReconciliation.getTo_acct_name());
                        json.setIsMatchCounterName("1");
                    } else {
                        json.setIsMatchCounterName("0");
                    }
                    json.setTran_amt(bankReconciliation.getTran_amt().abs().toString());
                    if (ac.getBillabstract() != null && ac.getBillabstract() == 1) {
                        json.setIsMatchRemark("1");
                        json.setRemark(bankReconciliation.getRemark());
                    } else {
                        json.setIsMatchRemark("0");
                    }
                    // 仅关联  财资统一对账码+本方账号+收支方向
                    if (bankReconciliation.getIsparsesmartcheckno()) {
                        json = new QuerySettleDeatailReqVO();
                        json.setBankaccount(bankReconciliation.getBankaccount());
                    }
                    json.setIsfuzzmatch(ac.getBillabstract().toString());
                    SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
                    //Date date = new Date();
                    if (ac.getFloatDays() != null) {
                        int floadaysadd = ac.getFloatDays().intValue();
                        int floadaysred = floadaysadd * (-1);
                        String startdate = format.format(DateUtils.dateAddDays(bankReconciliation.getTran_date(), floadaysred));
                        String enddate = format.format(DateUtils.dateAddDays(bankReconciliation.getTran_date(), floadaysadd));
                        json.setStartdate(startdate);
                        json.setEnddate(enddate);
                    }
                    String stwbDirection = ac.getDirection() == Direction.Debit.getValue() ? String.valueOf(Direction.Credit.getValue()) : String.valueOf(Direction.Debit.getValue());
                    json.setRecpaytype(stwbDirection);
//                                json.put("wdataOrigin", "8");
                    json.setAccentity(bankReconciliation.getAccentity());
                    //调用资金结算接口 -- 匹配资金结算单
                    if (!StringUtils.isEmpty(bankReconciliation.getSmartcheckno())) {
                        json.setCheckIdentificationCode(bankReconciliation.getSmartcheckno());
                    }
                    try {
                        ResultSettleDetailRespVO resJson = RemoteDubbo.get(SettleBenchBRPCService.class, IDomainConstant.MDD_DOMAIN_STWB).newQuerySettleBench(json);
                        // CZFW-496582 【DSP支持问题】部分银行流水未关联成功（流水交易日期早于结算成功日期），相关配置如下：1、自动关联规则已配置浮动天数2、调度任务条件已配置3、YMS放开数量限定（cmp.autoassociation.queryPageSize）已配置
                        // 结算单匹配到多个为错误，跳过该对账单
                        /*if (null != resJson.getDataList() && resJson.getDataList().size() > 1) {
                            CtmJSONObject logparam = new CtmJSONObject();
                            logparam.put("errorMsg", "结算单匹配到多个,跳过匹配，只能进行手工关联");
                            logparam.put("bankinfo", bankReconciliation);
                            logparam.put("reqVo", json);
                            logparam.put("settleRespVo", resJson);
                            ctmcmpBusinessLogService.saveBusinessLog(logparam, bankReconciliation.getBank_seq_no(), "银行流水自动关联", IServicecodeConstant.CMPBANKRECONCILIATION, "银行流水认领", "银行流水自动关联结算明细");
                            continue;
                        }*/
                        SettleDetailRespVO data = resJson.getData();
                        //自动关联结算明细业务日志添加，
                        CtmJSONObject logparam = new CtmJSONObject();
                        logparam.put("bankinfo", bankReconciliation);
                        logparam.put("reqVo", json);
                        logparam.put("settleRespVo", resJson);
                        ctmcmpBusinessLogService.saveBusinessLog(logparam, bankReconciliation.getBank_seq_no(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C0005D", "银行流水自动关联") /* "银行流水自动关联" */, IServicecodeConstant.CMPBANKRECONCILIATION, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C0005E", "银行流水认领") /* "银行流水认领" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C0005F", "银行流水自动关联结算明细") /* "银行流水自动关联结算明细" */);
                        if (data != null && data.getMainid() != null) {//匹配到结算单
                            // 匹配到一条结算单，判断金额是否向跟对账单一致，不一致跳过该对账单
                            if (bankReconciliation.getTran_amt().compareTo(data.getAmountmoney()) != 0) {
                                // ”流水支持处理方式“=’仅关联‘；
                                if (!bankReconciliation.getIsparsesmartcheckno()) {
                                    continue;
                                }
                                // 依据结算返回信息”是否换汇“字段，如为”是“，则直接匹配成功；如为”否“，则按照金额维度查询匹配，匹配成功直接关联
                                if (BooleanUtils.isFalse(data.getIsExchangePayment())) {
                                    continue;
                                }
                            }
                            // 财资统一对码相同
                            if (bankReconciliation.getIsparsesmartcheckno() && !bankReconciliation.getSmartcheckno().equals(data.getCheckIdentificationCode())) {
                                continue;
                            }
                            CorrDataEntity entity = new CorrDataEntity();
                            //将数据存入可关联实体
                            entity = setCorrDataFromJson(entity, bankReconciliation, data);
                            resList.add(entity);
                            noAssoDataFlag = false;
                            break;
                        }
                    } catch (Exception e) {
                        log.error("调用资金结算接口查询可关联数据报错！错误原因：", e);
                    }
                }
                // 仅关联只支持关联结算，如未关联，不进行后续关联
                if (BooleanUtils.toBoolean(bankReconciliation.getIsparsesmartcheckno()) && noAssoDataFlag) {
                    noAssoDataList.add(bankReconciliation);
                    continue;
                }
                //匹配资金收款单以及资金付款单
                String billType = null;
                String billNum = null;
                Map fund;
                List<Map> listfund = new ArrayList<Map>();
                if (Direction.Debit.getValue() == ac.getDirection() && BussDocumentType.fundpayment.getValue() == ac.getBusDocumentType()) {//匹配资金付款单
                    listfund = SqlHelper.selectList(AUTOCORRSETTINGLISTMAPPER + "getFundPaymentList", param);
                    billNum = IBillNumConstant.FUND_PAYMENT;
                    billType = String.valueOf(BussDocumentType.fundpayment.getValue());
                } else if (Direction.Credit.getValue() == ac.getDirection() && BussDocumentType.fundcollection.getValue() == ac.getBusDocumentType()) {//匹配资金收款单
                    listfund = SqlHelper.selectList(AUTOCORRSETTINGLISTMAPPER + "getFundCollectionList", param);
                    billNum = IBillNumConstant.FUND_COLLECTION;
                    billType = String.valueOf(BussDocumentType.fundcollection.getValue());
                } else if (Direction.Debit.getValue() == ac.getDirection() && BussDocumentType.transferaccount.getValue() == ac.getBusDocumentType() && !checkFundTransfer) { //匹配转账单 方向为 借：付
                    listfund = SqlHelper.selectList(AUTOCORRSETTINGLISTMAPPER + "getTransferAccountList_Debit", param);
                    billNum = IBillNumConstant.TRANSFERACCOUNT;
                    billType = String.valueOf(BussDocumentType.transferaccount.getValue());
                } else if (Direction.Credit.getValue() == ac.getDirection() && BussDocumentType.transferaccount.getValue() == ac.getBusDocumentType() && !checkFundTransfer) {//匹配转账单 方向为 贷：收
                    listfund = SqlHelper.selectList(AUTOCORRSETTINGLISTMAPPER + "getTransferAccountList_Credit", param);
                    billNum = IBillNumConstant.TRANSFERACCOUNT;
                    billType = String.valueOf(BussDocumentType.transferaccount.getValue());
                } else if (Direction.Debit.getValue() == ac.getDirection() && BussDocumentType.currencyexchange.getValue() == ac.getBusDocumentType()) {
                    //匹配外币兑换单 借
                    //银行对账单.银行对账码=外币兑换单.银行对账码,优先按照此规则进行匹配
                    if (null != param.get("bankcheckcode")) {
                        listfund = SqlHelper.selectList(AUTOCORRSETTINGLISTMAPPER + "getCurrencyexchangeListOne_Debit", param);
                    } else {
                        if (null != param.get("othBankNum_id")) {
                            listfund = SqlHelper.selectList(AUTOCORRSETTINGLISTMAPPER + "getCurrencyexchangeListTwo_Debit", param);
                        }
                    }
                    billNum = IBillNumConstant.CURRENCYEXCHANGE;
                    billType = String.valueOf(BussDocumentType.currencyexchange.getValue());
                } else if (Direction.Credit.getValue() == ac.getDirection() && BussDocumentType.currencyexchange.getValue() == ac.getBusDocumentType()) {
                    //匹配外币兑换单 贷
                    if (null != param.get("bankcheckcode")) {
                        listfund = SqlHelper.selectList(AUTOCORRSETTINGLISTMAPPER + "getCurrencyexchangeListOne_Credit", param);
                    } else {
                        if (null != param.get("othBankNum_id")) {
                            listfund = SqlHelper.selectList(AUTOCORRSETTINGLISTMAPPER + "getCurrencyexchangeListTwo_Credit", param);
                        }
                    }
                    billNum = IBillNumConstant.CURRENCYEXCHANGE;
                    billType = String.valueOf(BussDocumentType.currencyexchange.getValue());
                }
                if (listfund == null || listfund.size() < 1) continue;
                fund = listfund.get(0);
                CorrDataEntity entity = new CorrDataEntity();
                CtmJSONObject jsondata = new CtmJSONObject(fund);
                entity = setCorrData(entity, bankReconciliation, jsondata, billType, billNum);//将收款单、付款单、转账单匹配到的单据数据赋值到可关联实体
                resList.add(entity);
                log.error("匹配到资金单据，匹配到的对账单id：" + entity.getBankReconciliationId() + ";index:" + bankReconciliation.get("index") + ";总计：" + resList.size());
                noAssoDataFlag = false;
                break;
            }
            if (noAssoDataFlag) {
                noAssoDataList.add(bankReconciliation);
            }
        } catch (Exception e) {
            log.error("执行关联的数据报错，错误原因：", e);
        }
    }

    /**
     * 获取会计主体和“自动关联是否严格匹配”参数映射
     *
     * @param bankReconciliations
     * @return
     * @throws Exception
     */
    private Map<String, Boolean> getAccurateAutoassociateMap(List<BankReconciliation> bankReconciliations) throws Exception {
        List<String> accentityList = bankReconciliations.stream().map(s -> s.getAccentity()).collect(Collectors.toList());
        HashSet<String> accentitySet = new HashSet<>();
        for (String bankId : accentityList) {
            accentitySet.add(bankId);
        }
        //获取现金参数自动配置信息表标识字段
        QuerySchema querySchema = QuerySchema.create().addSelect("accentity,accurateautoassociate");
        QueryConditionGroup condition = QueryConditionGroup.and(
                //会计主体参数赋值
                QueryCondition.name("accentity").in(accentitySet)
        );
        querySchema.addCondition(condition);
        List<AutoConfig> autoConfigList = MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME, querySchema, null);
        Map<String, Boolean> retMap = autoConfigList.stream().collect(Collectors.toMap(AutoConfig::getAccentity, AutoConfig::getAccurateautoassociate));
        return retMap;
    }

    /**
     * 手动关联数据匹配
     *
     * @param isClaim
     * @param billType
     * @param bid
     * @param busid
     * @param pubts
     * @param pubts1
     * @return
     * @throws Exception
     */
    @Override
    public List<CorrDataEntity> manualAssociatedData(String isClaim, String billType, Long bid, Long busid, Date pubts, Date pubts1, Short dcFlag) throws Exception {
        FundPayment_b fundPayment;
        FundCollection_b fundCollection;
        TransferAccount transferAccount;
        CorrDataEntity entity = new CorrDataEntity();
        List<CorrDataEntity> resList = new ArrayList<CorrDataEntity>();
        String smartCheckNo = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();
        if ("1".equals(billType)) { //资金付款单
            List<FundPayment_b> fundPaymentList;
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(busid.toString()));
            querySchema.addCondition(group);
            fundPaymentList = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, querySchema, null);
            if (fundPaymentList != null && fundPaymentList.size() > 0) {
                fundPayment = fundPaymentList.get(0);
                String id = fundPayment.getMainid();
                QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("id").eq(id));
                querySchema1.addCondition(group1);
                List<FundPayment> fundPaymentList1 = MetaDaoHelper.queryObject(FundPayment.ENTITY_NAME, querySchema1, null);
                if (fundPaymentList1 != null && fundPaymentList1.size() > 0) {
                    //校验pubts单据是否是最新状态
                    Date fundMainpubts = fundPaymentList1.get(0).getPubts();
                    Date fundpubts = fundPayment.getPubts();
                    if (fundpubts.compareTo(pubts) != 0 || fundMainpubts.compareTo(pubts1) != 0) {
                        throw new CtmException(new CtmErrorCode("033-502-101268"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000E5", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000E5", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */) /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                    }
                    entity.setAuto(false);
                    entity.setBillType(String.valueOf(BussDocumentType.fundpayment.getValue()));
                    entity.setBusid(Long.valueOf(fundPayment.getId().toString()));
                    entity.setCode(fundPaymentList1.get(0).getCode());
                    entity.setBillNum(IBillNumConstant.FUND_PAYMENT);
                    entity.setDept(fundPayment.getDept());
                    entity.setMainid(Long.valueOf(id));
                    entity.setProject(fundPayment.getProject());
                    entity.setVouchdate(fundPaymentList1.get(0).getVouchdate());
                    entity.setOriSum(fundPayment.getOriSum());
                    entity.setAccentity(fundPaymentList1.get(0).getAccentity());
                }
            }
        } else if ("0".equals(billType)) { //资金收款单
            List<FundCollection_b> fundCollectionList;
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(busid.toString()));
            querySchema.addCondition(group);
            fundCollectionList = MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, querySchema, null);
            if (fundCollectionList != null && fundCollectionList.size() > 0) {
                fundCollection = fundCollectionList.get(0);
                String id = fundCollection.getMainid();
                QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("id").eq(id));
                querySchema1.addCondition(group1);
                List<FundCollection> fundCollectionList1 = MetaDaoHelper.queryObject(FundCollection.ENTITY_NAME, querySchema1, null);
                if (fundCollectionList1 != null && fundCollectionList1.size() > 0) {
                    //校验pubts单据是否是最新状态
                    Date fundMainpubts = fundCollectionList1.get(0).getPubts();
                    Date fundpubts = fundCollection.getPubts();
                    if (fundpubts.compareTo(pubts) != 0 || fundMainpubts.compareTo(pubts1) != 0) {
                        throw new CtmException(new CtmErrorCode("033-502-101268"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000E5", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000E5", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */) /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                    }
                    entity.setAuto(false);
                    entity.setBillType(String.valueOf(BussDocumentType.fundcollection.getValue()));
                    entity.setBusid(Long.parseLong(fundCollection.getId().toString()));
                    entity.setCode(fundCollectionList1.get(0).getCode());
                    entity.setBillNum(IBillNumConstant.FUND_COLLECTION);
                    entity.setDept(fundCollection.getDept());
                    entity.setMainid(Long.valueOf(id));
                    entity.setProject(fundCollection.getProject());
                    entity.setVouchdate(fundCollectionList1.get(0).getVouchdate());
                    entity.setAccentity(fundCollectionList1.get(0).getAccentity());
                    entity.setOriSum(fundCollection.getOriSum());
                }
            }
        } else if ("2".equals(billType)) { //转账单
            //转账单没有子表 busid 前端传过来的是个主表id
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(busid));
            querySchema.addCondition(group);
            List<TransferAccount> transferAccountList = MetaDaoHelper.queryObject(TransferAccount.ENTITY_NAME, querySchema, null);
            if (transferAccountList != null && transferAccountList.size() > 0) {
                transferAccount = transferAccountList.get(0);
                //校验pubts单据是否是最新状态
                if (transferAccount.getPubts().compareTo(pubts) != 0) {
                    throw new CtmException(new CtmErrorCode("033-502-101268"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000E5", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000E5", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */) /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
                // 相比资金收付款单 子表id  部门 转账单都不存在 无需进行set值
                entity.setAuto(false);
                entity.setBusid(transferAccount.getId());
                entity.setBillType(String.valueOf(BussDocumentType.transferaccount.getValue()));
                entity.setCode(transferAccount.getCode());
                entity.setBillNum(IBillNumConstant.TRANSFERACCOUNT);
                entity.setMainid(transferAccount.getId());
                entity.setProject(transferAccount.getProject());
                entity.setVouchdate(transferAccount.getVouchdate());
                entity.setAccentity(transferAccount.getAccentity());
                entity.setOriSum(transferAccount.getOriSum());
                //set 借贷方向
                entity.setDcFlag(dcFlag);
            }

        } else if ("14".equals(billType) && !"Claim".equals(isClaim)) { //外币兑换单
            //银行对账单页面传输的bid是银行对账单的id，busid是外币兑换单的id
            QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(bid));
            querySchema.addCondition(group);
            //查询银行对账单
            List<BankReconciliation> bankReconciliation = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
            if (bankReconciliation != null && bankReconciliation.size() > 0) {
                //外币兑换单查询条件
                QuerySchema querySchemaExchange = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
                QueryConditionGroup groupExchange = QueryConditionGroup.and(QueryCondition.name("id").eq(busid));
                querySchemaExchange.addCondition(groupExchange);
                //查询得到外币兑换单数据
                List<CurrencyExchange> currencyExchangeList = MetaDaoHelper.queryObject(CurrencyExchange.ENTITY_NAME, querySchemaExchange, null);

                if (currencyExchangeList != null && currencyExchangeList.size() > 0) {

                    short flag = bankReconciliation.get(0).getDc_flag().getValue();

                    //遍历外币兑换单,生成关联记录
                    for (CurrencyExchange currencyExchangeNew : currencyExchangeList) {
                        //设置外币兑换单id
                        entity.setMainid(busid);
                        //借贷方向
                        entity.setDcFlag(flag);
                        //业务单据ID=外币兑换单.ID
                        entity.setBusid(currencyExchangeNew.getId());
                        //关联类型=手动关联
                        entity.setAuto(false);// 是否自动
                        //业务单据类型=外币兑换单
                        entity.setBillType(String.valueOf(BussDocumentType.currencyexchange.getValue()));
                        //业务单据编号=外币兑换单.编号
                        entity.setBillNum(IBillNumConstant.CURRENCYEXCHANGE);
                        entity.setCode(currencyExchangeNew.getCode());
                        //业务单据日期=外币兑换单.单据日期
                        entity.setVouchdate(currencyExchangeNew.getVouchdate());
                        //项目=空
                        entity.setProject("");
                        //部门=空
                        //状态=已确认
                        //业务单元=外币兑换单.会计主体
                        entity.setAccentity(currencyExchangeNew.getAccentity());
                        //银行对账单id
                        entity.setBankReconciliationId(bid);
                        entity.setDept(currencyExchangeNew.getDept());
                        entity.setProject(currencyExchangeNew.getProject());
                        // 金额
                        if (flag == Direction.Credit.getValue()) {
                            entity.setOriSum(currencyExchangeNew.getPurchaseamount());
                        } else {
                            entity.setOriSum(currencyExchangeNew.getSellamount());
                        }
                    }
                }
            }
        } else if ("10".equals(billType)) { //付款工作台-付款单
            //付款工作台关联主表 busid 前端传过来的是个主表id
            PayBill payBill = MetaDaoHelper.findById(PayBill.ENTITY_NAME, busid);
            if (payBill != null) {
                //校验pubts单据是否是最新状态
                if (payBill.getPubts().compareTo(pubts) != 0) {
                    throw new CtmException(new CtmErrorCode("033-502-101268"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000E5", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000E5", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */) /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
                //关联数据赋值
                entity.setAuto(false);
                entity.setBusid(payBill.getId());
                entity.setBillType(String.valueOf(BussDocumentType.Payment.getValue()));
                entity.setCode(payBill.getCode());
                entity.setBillNum(IBillNumConstant.PAYMENT);
                entity.setMainid(payBill.getId());
                entity.setProject(payBill.getProject());
                entity.setDept(payBill.getDept());
                entity.setVouchdate(payBill.getVouchdate());
                entity.setAccentity(payBill.getAccentity());
                entity.setOriSum(payBill.getOriSum());
                entity.setDcFlag(dcFlag);
            }
        } else if ("7".equals(billType)) { //付款工作台-收款单
            //付款工作台关联主表 busid 前端传过来的是个主表id
            ReceiveBill receiveBill = MetaDaoHelper.findById(ReceiveBill.ENTITY_NAME, busid);
            if (receiveBill != null) {
                //校验pubts单据是否是最新状态
                if (receiveBill.getPubts().compareTo(pubts) != 0) {
                    throw new CtmException(new CtmErrorCode("033-502-101268"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000E5", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000E5", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */) /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
                //关联数据赋值
                entity.setAuto(false);
                entity.setBusid(receiveBill.getId());
                entity.setBillType(String.valueOf(BussDocumentType.ReceiveBill.getValue()));
                entity.setCode(receiveBill.getCode());
                entity.setBillNum(IBillNumConstant.RECEIVE_BILL);
                entity.setMainid(receiveBill.getId());
                entity.setProject(receiveBill.getProject());
                entity.setDept(receiveBill.getDept());
                entity.setVouchdate(receiveBill.getVouchdate());
                entity.setAccentity(receiveBill.getAccentity());
                entity.setOriSum(receiveBill.getOriSum());
                entity.setDcFlag(dcFlag);
            }
        }


        if ("Claim".equals(isClaim)) {
            //认领单
            //我的认领页面传输的bid为认领单id，busid为外币兑换单id
            Long claimId = bid;
            List<BillClaimItem> list;
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(bid));
            querySchema.addCondition(group);
            list = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema, null);
            //智能对账：判断是否是部分认领
            //BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, bid);

            if (list != null && list.size() > 0) {
                for (BillClaimItem billClaimItem : list) {
                    CorrDataEntity corr = new CorrDataEntity();
                    BeanUtils.copyProperties(entity, corr);
                    List<BankReconciliation> bankReconciliations;
                    bid = billClaimItem.getBankbill();
                    QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                    QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("id").eq(bid));
                    querySchema1.addCondition(group1);
                    bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema1, null);
                    //修复认领V1，合并认领后只处理第一条银行对账单的bug
                    if (bankReconciliations != null && bankReconciliations.size() > 0) {
                        for (BankReconciliation bankReconciliation : bankReconciliations) {
                            if ("14".equals(billType)) {
                                //外币兑换单查询条件
                                QuerySchema querySchemaExchange = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
                                QueryConditionGroup groupExchange = QueryConditionGroup.and(QueryCondition.name("id").eq(busid));
                                querySchemaExchange.addCondition(groupExchange);
                                //查询得到外币兑换单数据
                                List<CurrencyExchange> currencyExchangeList = MetaDaoHelper.queryObject(CurrencyExchange.ENTITY_NAME, querySchemaExchange, null);

                                if (currencyExchangeList != null && currencyExchangeList.size() > 0) {
                                    CurrencyExchange currencyExchange = currencyExchangeList.get(0);

                                    //获取借贷方向
                                    short flag = bankReconciliation.getDc_flag().getValue();

                                    if (flag == Direction.Credit.getValue()) {
                                        //1为借，2为贷
                                        corr.setOriSum(currencyExchange.getPurchaseamount());
                                    } else {
                                        corr.setOriSum(currencyExchange.getSellamount());//卖出金额
                                    }

                                    //借贷方向
                                    corr.setDcFlag(flag);
                                    //业务单据ID=外币兑换单.ID
                                    corr.setBusid(currencyExchange.getId());
                                    //关联类型=手动关联
                                    corr.setAuto(false);
                                    //业务单据类型=外币兑换单
                                    corr.setBillType(String.valueOf(BussDocumentType.currencyexchange.getValue()));
                                    //业务单据编号=外币兑换单.编号
                                    corr.setCode(currencyExchange.getCode());
                                    //业务单据日期=外币兑换单.单据日期
                                    corr.setVouchdate(currencyExchange.getVouchdate());
                                    //项目=空
                                    //部门=空
                                    //状态=已确认
                                    //业务单元=外币兑换单.会计主体
                                    corr.setAccentity(currencyExchange.getAccentity());
                                    //相比资金收付款单 子表id  部门 项目 转账单都不存在 无需进行set值
                                    corr.setBillNum(IBillNumConstant.CURRENCYEXCHANGE);
                                    //设置id
                                    corr.setMainid(claimId);
                                    //外币兑换单id
                                    corr.setBusid(busid);
                                }
                            }
                            corr.setBillClaimItemId(billClaimItem.getMainid());
                            corr.setBankReconciliationId(bankReconciliation.getId());
                            // 合并场景：合并后生成一个新对账码，同时更新交易流水上的统一对账码；新对账码传递至下游
                            if (StringUtils.isNotBlank(bankReconciliation.getSmartcheckno())) {
                                smartCheckNo = bankReconciliation.getSmartcheckno();
                            }
                            corr.setSmartcheckno(smartCheckNo);
                            resList.add(corr);
                        }
                    }
                }
            }
        } else {
            List<BankReconciliation> list;
            QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
            QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("id").eq(bid));
            querySchema1.addCondition(group1);
            list = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema1, null);
            if (list != null && list.size() > 0) {
                BankReconciliation bankReconciliation = list.get(0);
                entity.setBankReconciliationId(bankReconciliation.getId());
                //智能对账
                //CZFW-361041 现金业务单据手动关联时，财资统一对账码不为空，则沿用银行对账单的勾兑码
                if (StringUtils.isNotBlank(bankReconciliation.getSmartcheckno())) {
                    smartCheckNo = bankReconciliation.getSmartcheckno();
                }
                entity.setSmartcheckno(smartCheckNo);
                resList.add(entity);
            }
        }

        return resList;
    }

    /**
     * 设置手动关联资金结算明细数据
     *
     * @param map
     * @return
     * @throws Exception
     */
    @Override
    public List<CorrDataEntity> setAssociatedData(CtmJSONObject map) throws Exception {

        List<CorrDataEntity> corrDataEntities = new ArrayList<CorrDataEntity>();
        //CtmJSONObject jsonObject = new CtmJSONObject();
        Date vouchdate = map.getDate("vouchdate");
        String isClaim = map.getString("isClaim");
        Long bid = map.getLong("bid");

        CorrDataEntity corrEntity = new CorrDataEntity();
        corrEntity.setAccentity(map.getString(IBussinessConstant.ACCENTITY));
        corrEntity.setVouchdate(vouchdate);
        corrEntity.setBusid(map.getLong("busid"));
        corrEntity.setProject(map.getString("project"));
        corrEntity.setDept(map.getString("dept"));
        corrEntity.setMainid(map.getLong("mainid"));
        corrEntity.setBillType(map.getString("billType"));
        corrEntity.setOriSum(map.getBigDecimal(IBussinessConstant.ORI_SUM));
        corrEntity.setAuto(false);
        corrEntity.setCode(map.getString("code"));
        String smartCheckNo = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();
        if ("Claim".equals(isClaim)) {//认领单匹配
            List<BillClaimItem> list;
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(bid));
            querySchema.addCondition(group);
            list = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema, null);
            //智能对账：判断是否是部分认领
            //BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, bid);
            if (list != null && list.size() > 0) {
                for (BillClaimItem billClaimItem : list) {
                    //需要新new一个实体并复制，避免指针引用错误bug
                    CorrDataEntity corr = new CorrDataEntity();
                    BeanUtils.copyProperties(corrEntity, corr);
                    List<BankReconciliation> bankReconciliations;
                    bid = billClaimItem.getBankbill();
                    QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                    QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("id").eq(bid));
                    querySchema1.addCondition(group1);
                    bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema1, null);
                    if (bankReconciliations != null && bankReconciliations.size() > 0) {
                        //修复认领V1，合并认领后只处理第一条银行对账单的bug
                        for (BankReconciliation bankReconciliation : bankReconciliations) {
                            corr.setBillClaimItemId(billClaimItem.getMainid());
                            corr.setBankReconciliationId(bankReconciliation.getId());
                            // 更新交易流水上的统一对账码；新对账码传递至下游
                            if (StringUtils.isNotEmpty(bankReconciliation.getSmartcheckno())) {
                                smartCheckNo = bankReconciliation.getSmartcheckno();
                            }
                            //银行对账单存在勾兑码，则沿用
                            corr.setSmartcheckno(smartCheckNo);
                            corrDataEntities.add(corr);
                        }
                    }
                }
            }
        } else {
            BankReconciliation bankreconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, bid);
            if (bankreconciliation != null) {
                //智能对账
                //CZFW-361041 现金业务单据手动关联时，财资统一对账码不为空，则沿用银行对账单的勾兑码
                if (StringUtils.isNotBlank(bankreconciliation.getSmartcheckno())) {
                    smartCheckNo = bankreconciliation.getSmartcheckno();
                }
            }
            corrEntity.setSmartcheckno(smartCheckNo);
            corrEntity.setBankReconciliationId(bid);
            corrDataEntities.add(corrEntity);
        }
        return corrDataEntities;
    }

    @Override
    public HashMap<String, List<CorrDataEntity>> apiAssociatedData(BankReconciliation bankRec, CtmJSONObject relationItem) throws Exception {
        List<CorrDataEntity> innerList = new ArrayList<CorrDataEntity>();
        List<CorrDataEntity> outBillList = new ArrayList<CorrDataEntity>();
        HashMap<String, List<CorrDataEntity>> retMap = new HashMap<String, List<CorrDataEntity>>();
        //智能对账：生成勾兑码
//        String smartcheckno = UUID.randomUUID().toString().replace("-", "");
        //调用资金结算财资统一对账码接口生成
        String smartcheckno = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();
        boolean isOut = false;
        CorrDataEntity entity;
        String billType = relationItem.getString("billtype");
        //收款单，付款单，转账单，外币兑换单，资金收款单，资金付款单
        //关联子表：资金收款单 资金付款单
        //关联主表：转账单 外币兑换单 收款单 付款单
        // 暂时不支持：60资金结算明细，91融入登记, 93融资还本, 111融资付息, 112融资付费
//        if ("7".equals(billType)) { //收款单
//            entity = createApiCorrData4Receive(bankRec, relationItem);
//        } else if ("10".equals(billType)) { //付款单
//            entity = createApiCorrData4Payment(bankRec, relationItem);
//        } else
        switch (billType) {
            case "12":  //转账单
                entity = createApiCorrData4Transfer(bankRec, relationItem);
                break;
            case "14":  //外币兑换单
                entity = createApiCorrData4Exchange(bankRec, relationItem);
                break;
            case "17":  //资金收款单
                entity = createApiCorrData4FundCol(bankRec, relationItem);
                break;
            case "18":  //资金付款单
                entity = createApiCorrData4FundPay(bankRec, relationItem);
                break;
            default: //外部系统单据
                entity = createApiCorrData4OutBill(bankRec, relationItem);
                isOut = true;
                break;
        }
        entity.setBankReconciliationId(bankRec.getId());
        //智能对账
        entity.setSmartcheckno(smartcheckno);
        //特征
        if (relationItem.getObject("characterDef", LinkedHashMap.class) != null) {
            BizObject characterDef = Objectlizer.convert(relationItem.getObject("characterDef", LinkedHashMap.class), BankRecRelationCharacterDef.ENTITY_NAME);
            characterDef.put("id", ymsOidGenerator.nextId());
            //设置特征状态
            characterDef.setEntityStatus(EntityStatus.Insert);
            entity.setCharacterDef(characterDef);
        }

        //扩展字段
        HashMap<String, Object> extendFields = new HashMap<String, Object>();

        Set<Map.Entry<String, Object>> paramEntries = relationItem.entrySet();
        CtmJSONObject attrMap = relationItem.getJSONObject("extAttrMap");
        for (Map.Entry<String, Object> paramEntry : paramEntries) {
            if (paramEntry.getKey().startsWith("extend")) {
                if (attrMap != null && attrMap.get(paramEntry.getKey()) != null) {
                    if (DATA_TYPE_DOUBLE.equals(attrMap.getString(paramEntry.getKey()))) {
                        extendFields.put(paramEntry.getKey(), relationItem.getDouble(paramEntry.getKey()));
                    } else if (DATA_TYPE_FLOAT.equals(attrMap.getString(paramEntry.getKey()))) {
                        extendFields.put(paramEntry.getKey(), relationItem.getFloat(paramEntry.getKey()));
                    } else if (DATA_TYPE_BIGDECIMAL.equals(attrMap.getString(paramEntry.getKey()))) {
                        extendFields.put(paramEntry.getKey(), relationItem.getBigDecimal(paramEntry.getKey()));
                    } else if (DATA_TYPE_INTEGER.equals(attrMap.getInteger(paramEntry.getKey()))) {
                        extendFields.put(paramEntry.getKey(), relationItem.getInteger(paramEntry.getKey()));
                    } else if (DATA_TYPE_LONG.equals(attrMap.getString(paramEntry.getKey()))) {
                        extendFields.put(paramEntry.getKey(), relationItem.getLong(paramEntry.getKey()));
                    } else if (DATA_TYPE_SHORT.equals(attrMap.getString(paramEntry.getKey()))) {
                        extendFields.put(paramEntry.getKey(), relationItem.getShort(paramEntry.getKey()));
                    } else {
                        if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Double) {
                            extendFields.put(paramEntry.getKey(), relationItem.getDouble(paramEntry.getKey()));
                        } else if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Integer) {
                            extendFields.put(paramEntry.getKey(), relationItem.getInteger(paramEntry.getKey()));
                        } else if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Long) {
                            extendFields.put(paramEntry.getKey(), relationItem.getLong(paramEntry.getKey()));
                        } else if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Float) {
                            extendFields.put(paramEntry.getKey(), relationItem.getFloat(paramEntry.getKey()));
                        } else if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Short) {
                            extendFields.put(paramEntry.getKey(), relationItem.getShort(paramEntry.getKey()));
                        } else if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Date) {
                            extendFields.put(paramEntry.getKey(), relationItem.getDate(paramEntry.getKey()));
                        } else {
                            extendFields.put(paramEntry.getKey(), paramEntry.getValue());
                        }
                    }
                } else {
                    if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Double) {
                        extendFields.put(paramEntry.getKey(), relationItem.getDouble(paramEntry.getKey()));
                    } else if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Integer) {
                        extendFields.put(paramEntry.getKey(), relationItem.getInteger(paramEntry.getKey()));
                    } else if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Long) {
                        extendFields.put(paramEntry.getKey(), relationItem.getLong(paramEntry.getKey()));
                    } else if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Float) {
                        extendFields.put(paramEntry.getKey(), relationItem.getFloat(paramEntry.getKey()));
                    } else if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Short) {
                        extendFields.put(paramEntry.getKey(), relationItem.getShort(paramEntry.getKey()));
                    } else if (null != paramEntry.getValue() && paramEntry.getValue() instanceof Date) {
                        extendFields.put(paramEntry.getKey(), relationItem.getDate(paramEntry.getKey()));
                    } else {
                        extendFields.put(paramEntry.getKey(), paramEntry.getValue());
                    }
                }
            }
        }
        if (!extendFields.isEmpty()) {
            entity.setExtendFields(extendFields);
        }

        if (isOut) {
            outBillList.add(entity);
        } else {
            innerList.add(entity);
        }
        retMap.put("" + Relationtype.HeterogeneousAssociated.getValue(), outBillList);
        retMap.put("" + Relationtype.AutoAssociated.getValue(), innerList);
        return retMap;
    }

    private CorrDataEntity createApiCorrData4Transfer(BankReconciliation bankrec, CtmJSONObject relationItem) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("id,code,project,vouchdate,accentity,oriSum");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity").eq(bankrec.getAccentity()));
        conditionGroup.appendCondition(QueryCondition.name("code").eq(relationItem.getString("billcode")));
        if (!StringUtils.isEmpty(relationItem.getString("vouchdate"))) {
            conditionGroup.appendCondition(QueryCondition.name("vouchdate").eq(relationItem.getString("vouchdate")));
        }
        if (relationItem.getBigDecimal("amountmoney") != null) {
            conditionGroup.appendCondition(QueryCondition.name("oriSum").eq(relationItem.getBigDecimal("amountmoney")));
        }
        schema.addCondition(conditionGroup);
        List<TransferAccount> transferAccounts = MetaDaoHelper.queryObject(TransferAccount.ENTITY_NAME, schema, null);
        if (transferAccounts != null && transferAccounts.size() == 1) {
            TransferAccount transferAccount = transferAccounts.get(0);
            CorrDataEntity entity = new CorrDataEntity();
            entity.setApi(true);
            entity.setAuto(false);
            entity.setBusid(transferAccount.getId());
            entity.setBillType(String.valueOf(EventType.TransferAccount.getValue()));
            entity.setCode(transferAccount.getCode());
            entity.setBillNum(IBillNumConstant.TRANSFERACCOUNT);
            entity.setMainid(transferAccount.getId());
            entity.setProject(transferAccount.getProject());
            entity.setVouchdate(transferAccount.getVouchdate());
            entity.setAccentity(transferAccount.getAccentity());
            entity.setOriSum(transferAccount.getOriSum());
            //set 借贷方向
            entity.setDcFlag(bankrec.getDc_flag().getValue());
            return entity;
        } else {
            throw new CtmException(new CtmErrorCode("033-502-101152"), InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050016", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C0005B", "未找到唯一的转账单数据！") /* "未找到唯一的转账单数据！" */) /* "未找到唯一的转账单数据！" */);
        }
    }

    private CorrDataEntity createApiCorrData4Exchange(BankReconciliation bankrec, CtmJSONObject relationItem) throws Exception {
        short flag = bankrec.getDc_flag().getValue();
        QuerySchema schema = QuerySchema.create().addSelect("id,code,vouchdate,accentity,purchaseamount,sellamount");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity").eq(bankrec.getAccentity()));
        conditionGroup.appendCondition(QueryCondition.name("code").eq(relationItem.getString("billcode")));
        if (!StringUtils.isEmpty(relationItem.getString("vouchdate"))) {
            conditionGroup.appendCondition(QueryCondition.name("vouchdate").eq(relationItem.getString("vouchdate")));
        }
        if (relationItem.getBigDecimal("amountmoney") != null) {
            if (flag == 1) {
                //1为借，2为贷
                conditionGroup.appendCondition(QueryCondition.name("purchaseamount").eq(relationItem.getBigDecimal("amountmoney")));
            } else {
                conditionGroup.appendCondition(QueryCondition.name("sellamount").eq(relationItem.getBigDecimal("amountmoney")));
            }
        }
        schema.addCondition(conditionGroup);
        List<CurrencyExchange> exchanges = MetaDaoHelper.queryObject(CurrencyExchange.ENTITY_NAME, schema, null);
        if (exchanges != null && exchanges.size() == 1) {
            CurrencyExchange currencyExchangeNew = exchanges.get(0);
            CorrDataEntity entity = new CorrDataEntity();
            //金额=外币兑换单.卖出金额（方向为贷时）/外币兑换单.买入金额（方向为借时）
            if (flag == 1) {
                //1为借，2为贷
                entity.setOriSum(currencyExchangeNew.getPurchaseamount());
            } else {
                entity.setOriSum(currencyExchangeNew.getSellamount());//卖出金额
            }
            //设置外币兑换单id
            entity.setMainid(currencyExchangeNew.getId());
            //借贷方向
            entity.setDcFlag(flag);
            //业务单据ID=外币兑换单.ID
            entity.setBusid(currencyExchangeNew.getId());
            //关联类型=手动关联
            entity.setApi(true);
            entity.setAuto(true);
            //业务单据类型=外币兑换单
            entity.setBillType(String.valueOf(EventType.CurrencyExchangeBill.getValue()));
            //业务单据编号=外币兑换单.编号
            entity.setBillNum(IBillNumConstant.CURRENCYEXCHANGE);
            entity.setCode(currencyExchangeNew.getCode());
            //业务单据日期=外币兑换单.单据日期
            entity.setVouchdate(currencyExchangeNew.getVouchdate());
            //项目=空
            entity.setProject("");
            //部门=空
            //状态=已确认
            //业务单元=外币兑换单.会计主体
            entity.setAccentity(currencyExchangeNew.getAccentity());
            return entity;
        } else {
            throw new CtmException(new CtmErrorCode("033-502-101205"), InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050012", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C00058", "未找到唯一的外币兑换单数据！") /* "未找到唯一的外币兑换单数据！" */) /* "未找到唯一的外币兑换单数据！" */);
        }
    }

    private CorrDataEntity createApiCorrData4FundCol(BankReconciliation bankrec, CtmJSONObject relationItem) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("id,accentity,code,vouchdate");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity").eq(bankrec.getAccentity()));
        conditionGroup.appendCondition(QueryCondition.name("code").eq(relationItem.getString("billcode")));
        if (!StringUtils.isEmpty(relationItem.getString("vouchdate"))) {
            conditionGroup.appendCondition(QueryCondition.name("vouchdate").eq(relationItem.getString("vouchdate")));
        }
        schema.addCondition(conditionGroup);
        List<FundCollection> fundCols = MetaDaoHelper.queryObject(FundCollection.ENTITY_NAME, schema, null);
        if (fundCols != null && fundCols.size() == 1) {
            FundCollection fundColl = fundCols.get(0);
            QuerySchema subSchema = QuerySchema.create().addSelect("id,dept,project,oriSum");
            QueryConditionGroup subConditionGroup = new QueryConditionGroup(ConditionOperator.and);
            subConditionGroup.appendCondition(QueryCondition.name("mainid").eq(fundColl.getId()));
            if (!StringUtils.isEmpty(relationItem.getString("busid"))) {
                subConditionGroup.appendCondition(QueryCondition.name("id").eq(relationItem.getString("busid")));
            }
            if (relationItem.getBigDecimal("amountmoney") != null) {
                subConditionGroup.appendCondition(QueryCondition.name("oriSum").eq(relationItem.getBigDecimal("amountmoney")));
            }
            subSchema.addCondition(subConditionGroup);
            List<FundCollection_b> fundColBs = MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, subSchema, null);
            if (fundColBs == null || fundColBs.isEmpty()) {
                throw new CtmException(new CtmErrorCode("033-502-100091"), InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050015", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C00054", "未找到唯一的资金付款单数据！") /* "未找到唯一的资金付款单数据！" */) /* "未找到唯一的资金付款单数据！" */);
            }
            FundCollection_b fundCollB = fundColBs.get(0);
            CorrDataEntity entity = new CorrDataEntity();
            entity.setApi(true);
            entity.setAuto(true);
            entity.setBillType(String.valueOf(EventType.FundCollection.getValue()));
            entity.setBusid(Long.parseLong(fundCollB.get("id").toString()));
            entity.setCode(fundColl.getCode());
            entity.setBillNum(IBillNumConstant.FUND_PAYMENT);
            entity.setDept(fundCollB.getDept());
            entity.setMainid(Long.parseLong(fundCollB.get("id").toString()));
            entity.setProject(fundCollB.getProject());
            entity.setVouchdate(fundColl.getVouchdate());
            entity.setOriSum(fundCollB.getOriSum());
            entity.setAccentity(fundColl.getAccentity());
            return entity;
        } else {
            throw new CtmException(new CtmErrorCode("033-502-100091"), InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050015", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C00054", "未找到唯一的资金付款单数据！") /* "未找到唯一的资金付款单数据！" */) /* "未找到唯一的资金付款单数据！" */);
        }
    }

    private CorrDataEntity createApiCorrData4FundPay(BankReconciliation bankrec, CtmJSONObject relationItem) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("id,accentity,code,vouchdate");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("accentity").eq(bankrec.getAccentity()));
        conditionGroup.appendCondition(QueryCondition.name("code").eq(relationItem.getString("billcode")));
        if (!StringUtils.isEmpty(relationItem.getString("vouchdate"))) {
            conditionGroup.appendCondition(QueryCondition.name("vouchdate").eq(relationItem.getString("vouchdate")));
        }
        schema.addCondition(conditionGroup);
        List<FundPayment> fundPayments = MetaDaoHelper.queryObject(FundPayment.ENTITY_NAME, schema, null);
        if (fundPayments != null && fundPayments.size() == 1) {
            FundPayment fundPayment = fundPayments.get(0);
            QuerySchema subSchema = QuerySchema.create().addSelect("id,dept,project,oriSum");
            QueryConditionGroup subConditionGroup = new QueryConditionGroup(ConditionOperator.and);
            subConditionGroup.appendCondition(QueryCondition.name("mainid").eq(fundPayment.getId()));
            if (!StringUtils.isEmpty(relationItem.getString("busid"))) {
                subConditionGroup.appendCondition(QueryCondition.name("id").eq(relationItem.getString("busid")));
            }
            if (relationItem.getBigDecimal("amountmoney") != null) {
                subConditionGroup.appendCondition(QueryCondition.name("oriSum").eq(relationItem.getBigDecimal("amountmoney")));
            }
            subSchema.addCondition(subConditionGroup);
            List<FundPayment_b> fundPaymentBs = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, subSchema, null);
            if (fundPaymentBs == null || fundPaymentBs.isEmpty()) {
                throw new CtmException(new CtmErrorCode("033-502-100091"), InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050015", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C00054", "未找到唯一的资金付款单数据！") /* "未找到唯一的资金付款单数据！" */) /* "未找到唯一的资金付款单数据！" */);
            }
            FundPayment_b fundPaymentB = fundPaymentBs.get(0);
            CorrDataEntity entity = new CorrDataEntity();
            entity.setApi(true);
            entity.setAuto(true);
            entity.setBillType(String.valueOf(EventType.FundPayment.getValue()));
            entity.setBusid(Long.parseLong(fundPaymentB.get("id").toString()));
            entity.setCode(fundPayment.getCode());
            entity.setBillNum(IBillNumConstant.FUND_PAYMENT);
            entity.setDept(fundPaymentB.getDept());
            entity.setMainid(Long.parseLong(fundPaymentB.get("id").toString()));
            entity.setProject(fundPaymentB.getProject());
            entity.setVouchdate(fundPayment.getVouchdate());
            entity.setOriSum(fundPaymentB.getOriSum());
            entity.setAccentity(fundPayment.getAccentity());
            return entity;
        } else {
            throw new CtmException(new CtmErrorCode("033-502-100091"), InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050015", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C00054", "未找到唯一的资金付款单数据！") /* "未找到唯一的资金付款单数据！" */) /* "未找到唯一的资金付款单数据！" */);
        }
    }

    private CorrDataEntity createApiCorrData4OutBill(BankReconciliation bankrec, CtmJSONObject relationItem) throws Exception {
        CorrDataEntity entity = new CorrDataEntity();
        entity.setApi(true);
        entity.setAuto(true);
        entity.setOutBillTypeName(relationItem.getString("billtype"));
        entity.setCode(relationItem.getString("billcode"));
        entity.setVouchdate(relationItem.getDate("vouchdate"));
        entity.setOriSum(relationItem.getBigDecimal("amountmoney"));
        entity.setAccentity(bankrec.getAccentity());
        return entity;
    }

    /**
     * 获取自动关联银行对账单列表
     *
     * @param paramMap
     * @return
     */
    public List<BankReconciliation> getBankReconciliationList(Map<String, Object> paramMap) throws Exception {
        //根据调度任务参数条件查询对应的数据
        paramMap = paramMap == null ? new HashMap<>() : paramMap;
        //增加开始结束日期后，日期逻辑处理
        CtmJSONObject startAndEndResult = getStartAndEndDate(paramMap);
        String beginDateStr = startAndEndResult.getString("beginDate");
        String endDateStr = startAndEndResult.getString("endDate");
        CtmJSONObject param = new CtmJSONObject();
        param.put(TaskUtils.TASK_START_DATE,paramMap.get(TaskUtils.TASK_START_DATE));
        param.put(TaskUtils.TASK_END_DATE,paramMap.get(TaskUtils.TASK_END_DATE));
        param.put("daysinadvance",paramMap.get("dataRange"));
        HashMap<String, String> querydate = TaskUtils.queryDateProcess(param, "yyyy-MM-dd");
        Date startDate = DateUtils.dateParse(querydate.get(TaskUtils.TASK_START_DATE), DateUtils.DATE_PATTERN);
        Date endDate = DateUtils.dateParse(querydate.get(TaskUtils.TASK_END_DATE), DateUtils.DATE_PATTERN);
        if (startDate == null || endDate == null) {
            throw new com.yonyou.yonbip.ctm.error.CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C00059", "执行的调度任务配置的日期获取不对，请检查！") /* "执行的调度任务配置的日期获取不对，请检查！" */);
        }

        List<BankReconciliation> list;
        List<BankReconciliation> list2;
        Short isPublic = 0;
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group1 = null;
        //不包含日期范围相关参数时，不增加日期限制
        if (startAndEndResult.getBoolean("notContainDateParams")) {
            group1 = QueryConditionGroup.and(
                    QueryCondition.name("associationstatus").eq(0),
                    QueryCondition.name("ispublish").eq(0),
                    QueryCondition.name("checkflag").eq(0),
                    QueryCondition.name("other_checkflag").eq(0)
            );
        } else {
            group1 = QueryConditionGroup.and(
                    QueryCondition.name("associationstatus").eq(0),
                    QueryCondition.name("ispublish").eq(0),
                    QueryCondition.name("checkflag").eq(0),
                    QueryCondition.name("other_checkflag").eq(0),
                    QueryCondition.name("tran_date").between(startDate, endDate)
            );
        }
        //账户共享，使用组织不能为空
        group1.appendCondition(QueryCondition.name("accentity").is_not_null());
        //是否重复关联
        String repeatRelation = (String) (Optional.ofNullable(paramMap.get("repeatRelation")).orElse(""));
        if ("N".equals(repeatRelation)) {
            group1.appendCondition(QueryCondition.name("autoassociation").eq(0));
        }
        //收支方向
        String dc_flag = (String) (Optional.ofNullable(paramMap.get("dc_flag")).orElse(""));
        if (StringUtils.isNotEmpty(dc_flag)) {
            group1.appendCondition(QueryCondition.name("dc_flag").in(Arrays.asList(dc_flag.split(","))));
        }
        querySchema.addCondition(group1);
        int queryPageSize = Integer.parseInt(AppContext.getEnvConfig("cmp.autoassociation.queryPageSize", "5000"));
        querySchema.addPager(0, queryPageSize);
        //增加按照登账日期倒序查询
        querySchema.addOrderBy(new QueryOrderby("tran_date", "desc"));
        try {
            list = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        } catch (Exception e) {
            log.error("获取对账单列表错误" + e);
            return new ArrayList();
        }
        /**
         * 银行对账单已发布、未关联，认领单参照关联状态已关联，关联状态未关联的数据也进行自动关联操作
         * 1，查询认领单参照关联状态已关联，业务关联状态未关联对应的银行对账单id
         * 2，根据银行对账单id，查询银行对账单
         * 3, 整单认领、合并认领情况
         */
        // 1,根据认领单参照关联状态已关联，关联状态未关联对应的银行对账单id
        QuerySchema querySchemaClaim = QuerySchema.create().addSelect(" id,bankbill,mainid ");
        QueryConditionGroup group2 = QueryConditionGroup.and(
                QueryCondition.name("mainid.refassociationstatus").eq(1),
                QueryCondition.name("mainid.associationstatus").eq(0),
                QueryCondition.name("mainid.claimtype").not_eq(3)
        );
        querySchemaClaim.addCondition(group2);
        try {
            List<BillClaimItem> billClaimItems = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchemaClaim, null);
            List<Long> bankBillIds = new ArrayList<>();
            if (billClaimItems != null) {
                billClaimItems.forEach(billClaimItem -> {
                    bankBillIds.add(billClaimItem.getBankbill());
                });
            }
            if (bankBillIds.size() > 0) {
                QuerySchema schemaReconciliation = QuerySchema.create().addSelect(" * ");
                QueryConditionGroup group3 = QueryConditionGroup.and(
                        QueryCondition.name("id").in(bankBillIds),
                        QueryCondition.name("tran_date").between(beginDateStr, endDateStr),
                        // 未关联
                        QueryCondition.name("associationstatus").eq(0)

                );
                QueryConditionGroup group4 = QueryConditionGroup.and(
                        QueryCondition.name("id").in(bankBillIds),
                        QueryCondition.name("tran_date").between(beginDateStr, endDateStr),
                        // 已关联 入账类型为挂账
                        QueryCondition.name("associationstatus").eq(1),
                        QueryCondition.name("entrytype").eq(EntryType.CrushHang_Entry.getValue())
                );
                //是否重复关联
                if ("N".equals(repeatRelation)) {
                    group3.appendCondition(QueryCondition.name("autoassociation").eq(0));
                    group4.appendCondition(QueryCondition.name("autoassociation").eq(0));
                }
                //收支方向
                if (StringUtils.isNotEmpty(dc_flag)) {
                    group3.appendCondition(QueryCondition.name("dc_flag").in(Arrays.asList(dc_flag.split(","))));
                    group4.appendCondition(QueryCondition.name("dc_flag").in(Arrays.asList(dc_flag.split(","))));
                }
                QueryConditionGroup mainGroup = QueryConditionGroup.or(
                        group3,
                        group4
                );
                schemaReconciliation.addCondition(mainGroup);
                list2 = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schemaReconciliation, null);
                list.addAll(list2);
            }
        } catch (Exception e) {
            log.error("获取对账单列表错误2" + e);
            return new ArrayList<>();
        }

        // 自动关联逻辑，添加流水正在处理中的过滤
        List<BankReconciliation> realBankReconciliation = bankreconciliationService.filterBankReconciliationByLockKey(list, BankReconciliationActions.AutoAssociation);
        for (int i = 0; i < realBankReconciliation.size(); i++) {
            log.error("自动关联数据集合：" + i + ";" + realBankReconciliation.get(i).getId() + "共：" + realBankReconciliation.size());
            realBankReconciliation.get(i).set("index", i);
        }
        return realBankReconciliation;
    }

    /**
     * 处理银行流水自动关联调度任务中，日期浮动天数，开始结束日期的关系
     * @param paramMap
     * @return 结算所得的开始日期和结束日期
     */
    private CtmJSONObject getStartAndEndDate(Map<String, Object> paramMap){
        CtmJSONObject ctmJSONObject = new CtmJSONObject();
        // 校验参数逻辑
        String startDateStr = (String) paramMap.get("startdate");
        String endDateStr = (String) paramMap.get("enddate");
        Object dataRangeObj = paramMap.get("dataRange");
        // 判断 startDate 和 endDate 是否同时有值或同时为空
        boolean hasStartDate = StringUtils.isNotEmpty(startDateStr);
        boolean hasEndDate = StringUtils.isNotEmpty(endDateStr);
        if (hasStartDate != hasEndDate) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C0005A", "开始日期[startdate]和结束日期[enddate]必须同时有值或同时为空") /* "开始日期[startdate]和结束日期[enddate]必须同时有值或同时为空" */);
        }
        // 如果 startDate 和 endDate 有值，但 dataRange 也有值，则抛出异常
        if (hasStartDate && dataRangeObj != null && StringUtils.isNotEmpty(dataRangeObj.toString())) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C0005C", "当指定了开始日期[startdate]和结束日期[enddate]时，不能同时指定日期范围参数[dataRange]") /* "当指定了开始日期[startdate]和结束日期[enddate]时，不能同时指定日期范围参数[dataRange]" */);
        }
        if (hasStartDate && hasEndDate) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date startDate = sdf.parse(startDateStr);
                Date endDate = sdf.parse(endDateStr);
                if (startDate.after(endDate)) {
                    throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C00055", "开始日期[%s]不能大于结束日期[%s]") /* "开始日期[%s]不能大于结束日期[%s]" */, startDateStr, endDateStr));
                }
                // 增加判断：startDate 和 endDate 不能相差31天以上
                long diffInMillis = endDate.getTime() - startDate.getTime();
                long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);
                if (diffInDays > 31) {
                    throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C00056", "开始日期[%s]和结束日期[%s]之间不能相差31天以上") /* "开始日期[%s]和结束日期[%s]之间不能相差31天以上" */, startDateStr, endDateStr));
                }
            } catch (ParseException e) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C00057", "日期格式不正确，请使用 yyyy-MM-dd 格式") /* "日期格式不正确，请使用 yyyy-MM-dd 格式" */);
            }
        }
        // 时间范围
        Integer dataRange;
        if (dataRangeObj != null && StringUtils.isNotEmpty(dataRangeObj.toString())) {
            dataRange = Integer.valueOf(dataRangeObj.toString());
        } else {
            dataRange = -1;
        }
        // 计算交易日期范围 1130需求，业务关联优化，调度任务加日期范围
        Date beginDate = DateUtils.dateAddDays(new Date(), -dataRange);
        SimpleDateFormat beginSdf = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
        SimpleDateFormat endSdf = new SimpleDateFormat("yyyy-MM-dd 23:59:59");
        String beginDateStr;
        String endDateStr2;
        // 使用传入的 startDate 和 endDate，如果有的话
        if (hasStartDate && hasEndDate) {
            beginDateStr = startDateStr;
            endDateStr2 = endDateStr;
        } else {
            beginDateStr = beginSdf.format(beginDate);
            endDateStr2 = endSdf.format(new Date());
        }
        ctmJSONObject.put("beginDate", beginDateStr);
        ctmJSONObject.put("endDate", endDateStr2);
        //判断是否没有日期相关的参数，没有的话不限制日期
        ctmJSONObject.put("notContainDateParams", !hasStartDate && !hasEndDate && dataRangeObj == null);
        return ctmJSONObject;
    }

    /**
     * 获取自动关联规则列表
     *
     * @param bankReconciliation
     * @return
     */
    public List<Autocorrsetting> getAutoCorrSettingCredit(BankReconciliation bankReconciliation) {
        List<Autocorrsetting> list = new ArrayList<>();
        try {
            // todo *
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            //todo 备注 关键字  方法抽取
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("tenant").eq(bankReconciliation.getTenant()), QueryCondition.name("direction").eq(2));
            querySchema.addCondition(group);
            querySchema.addOrderBy("exorder");
            if (bankReconciliation != null) {
                list = MetaDaoHelper.queryObject(Autocorrsetting.ENTITY_NAME, querySchema, null);
            } else {
                list = new ArrayList<Autocorrsetting>();
            }
        } catch (Exception e) {
            log.error("获取自动关联设置错误" + e);
            list = new ArrayList<Autocorrsetting>();
        }
        return list;
    }

    /**
     * 获取自动关联规则列表
     *
     * @param bankReconciliation
     * @return
     */
    public List<Autocorrsetting> getAutoCorrSettingDebit(BankReconciliation bankReconciliation) {
        List<Autocorrsetting> list;
        try {
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("tenant").eq(bankReconciliation.getTenant()), QueryCondition.name("direction").eq(1));
            querySchema.addCondition(group);
            querySchema.addOrderBy("exorder");
            if (bankReconciliation != null) {
                list = MetaDaoHelper.queryObject(Autocorrsetting.ENTITY_NAME, querySchema, null);
            } else {
                list = new ArrayList<Autocorrsetting>();
            }
        } catch (Exception e) {
            log.error("获取自动关联设置错误" + e);
            list = new ArrayList<Autocorrsetting>();
        }
        return list;
    }

    /**
     * 根据自动关联规则获取匹配业务单据的数据
     *
     * @param ac
     * @param bankReconciliation
     * @return
     */
    public Map<String,Object> selectData(Autocorrsetting ac, BankReconciliation bankReconciliation) {
        Map<String,Object> bk = new HashMap();
        //模糊匹配摘要
        if (0 == ac.getBillabstract()) {
            bk.put("connetionWay", "0");
            if (bankReconciliation.getRemark() == null) {
                bk.put("connetionWay", "2");
            }
            bk.put("remark", bankReconciliation.getRemark());
        }
        //精确匹配摘要
        if (1 == ac.getBillabstract()) {
            bk.put("connetionWay", "1");
            if (bankReconciliation.getRemark() == null) {
                bk.put("connetionWay", "2");
            }
            bk.put("remark", bankReconciliation.getRemark());
        }
        //匹配对账编码
        if (1 == ac.getBankRecCode()) {
            bk.put("bankRecCode", bankReconciliation.getBankcheckno());
            //bk.put("bankcheckcode",bankReconciliation.getBankcheckno());
        }
        //匹配金额
        if (1 == ac.getMoney()) {
            bk.put("money", bankReconciliation.getTran_amt().abs());
        }
        //对方银行账号--精确匹配
        if (1 == ac.getOthBankNum()) {
            bk.put("connetionWay_OthBankNum", "1");
            if (bankReconciliation.getTo_acct_no() == null) {
                bk.put("connetionWay_OthBankNum", "0");
            }
            bk.put("othBankNum", bankReconciliation.getTo_acct_no());
            bk.put("othBankNum_id", bankReconciliation.getTo_acct());
        }
        //对方银行账号--无
        if (0 == ac.getOthBankNum()) {
            bk.put("connetionWay_OthBankNum", "0");
            if (bankReconciliation.getTo_acct_no() == null) {
                bk.put("connetionWay_OthBankNum", "0");
            }
            bk.put("othBankNum", bankReconciliation.getTo_acct_no());
            bk.put("othBankNum_id", bankReconciliation.getTo_acct());
        }
        //对方开户名--精确匹配
        if (1 == ac.getOthBankNumName()) {
            bk.put("connetionWay_OthBankNumName", "1");
            if (bankReconciliation.getTo_acct_name() == null) {
                bk.put("connetionWay_OthBankNumName", "0");
            }
            bk.put("to_acct_name", bankReconciliation.getTo_acct_name());
        }
        //对方开户名--无
        if (0 == ac.getOthBankNumName()) {
            bk.put("connetionWay_OthBankNumName", "0");
            if (bankReconciliation.getTo_acct_name() == null) {
                bk.put("connetionWay_OthBankNumName", "0");
            }
            bk.put("to_acct_name", bankReconciliation.getTo_acct_name());
        }

        //我方银行账号
        if (1 == ac.getOurBankNum()) {
            bk.put("ourBankNum", bankReconciliation.getBankaccount());
        }
        //租户id
        bk.put("tenantid", bankReconciliation.getTenant());
        bk.put("ytenantid", InvocationInfoProxy.getTenantid());
        bk.put("tenantidb", bankReconciliation.getTenant());
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        //Date date = new Date();
        //浮动日期
        if (ac.getFloatDays() != null) {
            //获取浮动日期的天数
            int i = ac.getFloatDays().intValue();
            bk.put("startTime", format.format(DateUtils.dateAddDays(bankReconciliation.getTran_date(), -i)));
            bk.put("endTime", format.format(DateUtils.dateAddDays(bankReconciliation.getTran_date(), i)));
        }
        return bk;
    }

    /**
     * 关联实体赋值
     *
     * @param entity
     * @param bankReconciliation
     * @param jsondata
     * @param billType
     * @param billNum
     * @return
     */
    public CorrDataEntity setCorrData(CorrDataEntity entity, BankReconciliation bankReconciliation, CtmJSONObject jsondata, String billType, String billNum) {
        entity.setAuto(true);
        entity.setBillType(billType);
        entity.setBillNum(billNum);
        entity.setBankReconciliationId(bankReconciliation.getId());
        entity.setBusid(jsondata.getLong("id"));
        entity.setAccentity(bankReconciliation.getAccentity());
        entity.setCode(jsondata.getString("code"));
        entity.setDept(jsondata.getString("dept"));
        entity.setMainid(jsondata.getLong("mainid"));
        entity.setProject(jsondata.getString("project"));
        entity.setVouchdate(jsondata.getDate("vouchdate"));
        entity.setOriSum(jsondata.getBigDecimal(IBussinessConstant.ORI_SUM));
        entity.setDcFlag(bankReconciliation.getDc_flag().getValue());
        if ("4".equals(billType)) {
            //当为外币兑换单时，金额=外币兑换单.卖出金额（方向为贷时）/外币兑换单.买入金额（方向为借时）
            if (bankReconciliation.getDc_flag().getValue() == Direction.Credit.getValue()) {
                entity.setOriSum(jsondata.getBigDecimal("purchaseamount"));
            } else {
                entity.setOriSum(jsondata.getBigDecimal("sellamount"));
            }
        }
        return entity;
    }

    /**
     * 将数据存入可编辑实体
     *
     * @param entity
     * @param bankReconciliation
     * @param data
     * @return
     */
    public CorrDataEntity setCorrDataFromJson(CorrDataEntity entity, BankReconciliation bankReconciliation, SettleDetailRespVO data) {
        this.buildCorrDataEntiry(entity,
                true, BussDocumentType.settlebench.getValue() + "", bankReconciliation.getId(), data.getId(), bankReconciliation.getAccentity(),
                data.getCode(), fundsettlement, data.getDept(), data.getMainid(), data.getProject(),
                data.getVouchdate(), data.getAmountmoney(), data.getCheckIdentificationCode());
        return entity;
    }

    @Override
    public void buildCorrDataEntiry(CorrDataEntity entity,
                                    boolean auto, String billType, Long bankReconciliationId, Long busiId, String accentity,
                                    String code, String billNum, String dept, Long mainId, String project,
                                    Date vouchdate, BigDecimal oriSum, String smartCheckNo) {
        entity.setAuto(auto);
        entity.setBillType(billType);
        entity.setBankReconciliationId(bankReconciliationId);
        entity.setBusid(busiId);
        entity.setAccentity(accentity);
        entity.setCode(code);
        entity.setBillNum(billNum);
        entity.setDept(dept);
        entity.setMainid(mainId);
        entity.setProject(project);
        entity.setVouchdate(vouchdate);
        entity.setOriSum(oriSum);
        entity.setSmartcheckno(smartCheckNo);
    }

    @Override
    public CtmJSONObject initAutoCorrSetting(Tenant tenant) throws Exception{
        CtmJSONObject result = new CtmJSONObject();
        //判断是否已经有数据
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        //业务单据类型=2
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("ytenantId").eq(tenant.getYTenantId())
        );
        querySchema.addCondition(group);
        List<Autocorrsetting> list = MetaDaoHelper.queryObject(Autocorrsetting.ENTITY_NAME, querySchema, null);
        //存在数据则删除历史数据，重新初始化
        if (list != null && list.size()>0){
            MetaDaoHelper.delete(Autocorrsetting.ENTITY_NAME, list);
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("tenantid", tenant.getId());
        map.put("ytenantid", tenant.getYTenantId());
        long idDebit = ymsOidGenerator.nextId();
        long idCredit = ymsOidGenerator.nextId();
        map.put("idDebit", idDebit);
        map.put("idCredit", idCredit);
        map.put("pubts", new Date());
        Object obj = SqlHelper.selectFirst("com.yonyoucloud.fi.ficm.mapper.initficmMapper.getAutoCorrSetting", map);
        if (obj == null) {
            SqlHelper.insert("com.yonyoucloud.fi.ficm.mapper.initficmMapper.initAutoCorrSetting", map);
        }
        result.put("success", true);
        return result;
    }

    @Override
    public List<CorrDataEntity> autoCorrCurrencyExchangeData(List<BankReconciliation> bankReconciliationList,Map<String, Object> paramMap) throws Exception {
        //20260415 只处理银行流水和货币兑换的自动关联
        ExecutorService autoAssociatedDataExecutor = null;
        Vector<CorrDataEntity> resList = new Vector<>();
        try {
            Vector<BankReconciliation> noAssoDataList = new Vector();
            Autocorrsetting autocorrsetting = initAutoCorrSettingByParams(paramMap);
            if (bankReconciliationList != null && bankReconciliationList.size() > 0) {
                try {
                    //适配自动任务进行关联操作
                    autoAssociatedDataExecutor = ThreadPoolBuilder.defaultThreadPoolBuilder().builder(10, 10, 10000, "autoCorrCurrencyExchangeData");
                    ExecutorService finalAutoAssociatedDataExecutor = autoAssociatedDataExecutor;
                    List<? extends Future<?>> futures = bankReconciliationList.stream().map(bankReconciliation -> {
                        return finalAutoAssociatedDataExecutor.submit(() -> {
                            // 这里只是查询数据，不涉及数据库操作
                            ergodicCorrCurrencyExchangeData(bankReconciliation, autocorrsetting, resList, noAssoDataList);
                        });
                    }).collect(Collectors.toList());
                    futures.forEach(future -> {
                        try {
                            future.get();
                        } catch (Exception e) {
                            log.error("银行流水自动关联货币兑换失败！，失败原因：", e);
                        }
                    });
                    log.error("银行流水自动关联货币兑换数据匹配！已关联数据量：" + resList.size());
                    log.error("银行流水自动关联货币兑换数据匹配！未关联数据量：" + noAssoDataList.size());
                    // 以下单起线程更新未命中的流水自动关联任务标识
                    if (CollectionUtils.isNotEmpty(noAssoDataList)) {
                        autoAssociatedDataExecutor.submit(() -> {
                            try {
                                CommonSaveUtils.repairBankReconciliationSatus(noAssoDataList);
                                autoCorrService.updateBankRecAutoassociation(noAssoDataList);
                            } catch (Exception e) {
                                log.error("更新自动关联任务标识失败！失败原因：", e);
                            }
                        });
                    }
                } catch (Exception e) {
                    log.error("执行自动关联数据匹配任务失败", e);
                    throw e;
                } finally {
                    if (autoAssociatedDataExecutor != null) {
                        autoAssociatedDataExecutor.shutdown();
                    }
                }
            }
        } finally {
            if (autoAssociatedDataExecutor != null) {
                autoAssociatedDataExecutor.shutdown();
            }
        }
        return resList;
    }

    /**
     * 根据自动关联货币兑换调度任务入参，初始化自动关联规则
     * @param paramMap 调度任务参数
     * @return 自动关联规则
     */
    private Autocorrsetting initAutoCorrSettingByParams(Map<String, Object> paramMap){
        Autocorrsetting autocorrsetting = new Autocorrsetting();
        //金额，本方账号，默认匹配
        autocorrsetting.setMoney((short) 1);
        autocorrsetting.setOurBankNum((short) 1);
        autocorrsetting.setBankRecCode((short) 1);
        //对方银行账号 匹配方式
        if(!StringUtils.isEmpty(paramMap.containsKey("otherbankaccountFlag")? paramMap.get("otherbankaccountFlag").toString() : null)){
            autocorrsetting.setOthBankNum(Short.valueOf(
                    MATCH_TYPE_MAP.getOrDefault(paramMap.get("otherbankaccountFlag").toString(), "0")
            ));
        }
        //对方户名 匹配方式
        if(!StringUtils.isEmpty(paramMap.containsKey("otherbanknameFlag")? paramMap.get("otherbanknameFlag").toString() : null)){
            autocorrsetting.setOthBankNumName(Short.valueOf(
                    MATCH_TYPE_MAP.getOrDefault(paramMap.get("otherbanknameFlag").toString(), "0")
            ));
        }
        //摘要 匹配方式
        if(!StringUtils.isEmpty(paramMap.containsKey("remarkMatchFlag")? paramMap.get("remarkMatchFlag").toString() : null)){
            autocorrsetting.setBillabstract(Short.valueOf(
                    REMARK_MATCH_TYPE_MAP.getOrDefault(paramMap.get("remarkMatchFlag").toString(), "2")
            ));
        }
        //日期浮动天数
        if (paramMap.containsKey("datefloats")){
            autocorrsetting.setFloatDays(new BigDecimal(paramMap.get("datefloats").toString()));
        }else {
            autocorrsetting.setFloatDays(new BigDecimal(0));
        }
        return autocorrsetting;
    }

    // 定义静态常量 Map；调度任务展示中文，数据库需要对应的数字
    private static final Map<String, String> MATCH_TYPE_MAP = new HashMap<String, String>() {{
        put("不匹配", "0");//@notranslate
        put("匹配", "1");//@notranslate
    }};

    private static final Map<String, String> REMARK_MATCH_TYPE_MAP = new HashMap<String, String>() {{
        put("模糊匹配", "0");//@notranslate
        put("匹配", "1");//@notranslate
        put("不匹配", "2");//@notranslate
    }};

    private void ergodicCorrCurrencyExchangeData(BankReconciliation bankReconciliation, Autocorrsetting autocorrsetting, Vector<CorrDataEntity> resList, Vector<BankReconciliation> noAssoDataList) {
        boolean noAssoDataFlag = true;
        try {
            Map<String,Object> param = selectData(autocorrsetting, bankReconciliation);//获取拼接匹配业务单据的参数
            // 仅关联只支持关联结算，如未关联，不进行后续关联
            if (BooleanUtils.toBoolean(bankReconciliation.getIsparsesmartcheckno()) && noAssoDataFlag) {
                noAssoDataList.add(bankReconciliation);
            }
            //匹配资金收款单以及资金付款单
            String billType = null;
            String billNum = null;
            Map fund;
            List<Map> listfund = new ArrayList<Map>();
           if (Direction.Debit.getValue() == bankReconciliation.getDc_flag().getValue()) {
                //匹配外币兑换单 借
                //银行对账单.银行对账码=外币兑换单.银行对账码,优先按照此规则进行匹配
                if (null != param.get("bankcheckcode")) {
                    listfund = SqlHelper.selectList(AUTOCORRSETTINGLISTMAPPER + "getCurrencyexchangeListOne_Debit", param);
                } else {
                    if (null != param.get("othBankNum_id")) {
                        listfund = SqlHelper.selectList(AUTOCORRSETTINGLISTMAPPER + "getCurrencyexchangeListTwo_Debit", param);
                    }
                }
                billNum = IBillNumConstant.CURRENCYEXCHANGE;
                billType = String.valueOf(BussDocumentType.currencyexchange.getValue());
            } else if (Direction.Credit.getValue() == bankReconciliation.getDc_flag().getValue()) {
                //匹配外币兑换单 贷
                if (null != param.get("bankcheckcode")) {
                    listfund = SqlHelper.selectList(AUTOCORRSETTINGLISTMAPPER + "getCurrencyexchangeListOne_Credit", param);
                } else {
                    if (null != param.get("othBankNum_id")) {
                        listfund = SqlHelper.selectList(AUTOCORRSETTINGLISTMAPPER + "getCurrencyexchangeListTwo_Credit", param);
                    }
                }
                billNum = IBillNumConstant.CURRENCYEXCHANGE;
                billType = String.valueOf(BussDocumentType.currencyexchange.getValue());
            }
            if (listfund == null || listfund.size() < 1) {
                noAssoDataList.add(bankReconciliation);
                return;
            }
            fund = listfund.get(0);
            CorrDataEntity entity = new CorrDataEntity();
            CtmJSONObject jsondata = new CtmJSONObject(fund);
            entity = setCorrData(entity, bankReconciliation, jsondata, billType, billNum);//将货币兑换匹配到的单据数据赋值到可关联实体
            resList.add(entity);
            log.error("匹配到资金单据，匹配到的对账单id：" + entity.getBankReconciliationId() + ";index:" + bankReconciliation.get("index") + ";总计：" + resList.size());
        } catch (Exception e) {
            log.error("执行关联的数据报错，错误原因：", e);
        }
    }


}
