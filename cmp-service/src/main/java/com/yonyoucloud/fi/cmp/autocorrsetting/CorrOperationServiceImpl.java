package com.yonyoucloud.fi.cmp.autocorrsetting;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.ctm.stwb.reconcode.pubitf.ReconciliateCodeGenerator;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.ctm.stwb.api.settlebench.SettleBenchBRPCService;
import com.yonyoucloud.ctm.stwb.reqvo.SettleDeatailRelBankBillReqVO;
import com.yonyoucloud.ctm.stwb.respvo.ResultStrRespVO;
import com.yonyoucloud.ctm.stwb.settlebench.SettleBench_b;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.autocorrsettings.BussDocumentType;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.enums.ConfirmStatusEnum;
import com.yonyoucloud.fi.cmp.enums.SerialdealendState;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts.DealDetailEnumConst;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.CorrDataEntity;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.process.ProcessUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 对账单与认领单业务关联
 *
 * @author msc
 */
@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class CorrOperationServiceImpl implements CorrOperationService {
    {

    }
    private final String AUTOCORRSETTINGLISTMAPPER = "com.yonyoucloud.fi.cmp.mapper.AutoCorrSettingMapper.";
    private final Short auto = 0;
    private final Short mau = 1;
    private final Short corr = 1;
    private final Short refundStatus = 1; // 1 为疑似退票
    //融资还本单
    private final String tlm_repayment = "cmp_bankreconciliationlist_tlm_repayment";
    //融资付息单
    private final String tlm_payinterest = "cmp_bankreconciliationlist_tlm_payinterest";
    //融资付费单
    private final String tlm_financepayfee = "cmp_bankreconciliationlist_tlm_financepayfee";
    //融入登记单
    private final String tlm_financeinStatement = "cmp_bankreconciliationlist_tlm_financeinStatement";
    //投资收息单
    private final String tlm_interestcollection = "cmp_bankreconciliationlist_tlm_interestcollection";
    //投资管理-申购登记单
    private final String tlm_purchaseregister = "cmp_bankreconciliationlist_tlm_purchasepay";
    //投资赎回单
    private final String tlm_investredem = "cmp_bankreconciliationlist_tlm_investredem";
    //投资付费单
    private final String tlm_investpayment = "cmp_bankreconciliationlist_tlm_investpayment";
    //投资分红单
    private final String tlm_investprofitsharing = "cmp_bankreconciliationlist_tlm_investprofitsharing";
    //衍生品交易交割单
    private final String tlm_tradedelivery = "cmp_bankreconciliationlist_tlm_tradedelivery";
    //衍生品交易平仓单
    private final String tlm_derivativesclose = "cmp_bankreconciliationlist_tlm_derivativesclose";
    //衍生品交易展期登记单
    private final String tlm_traderolloverregister = "cmp_bankreconciliationlist_tlm_traderolloverregister";
    //衍生品交易保证金登记单
    private final String tlm_addbond = "cmp_bankreconciliationlist_tlm_addbond";
    @Autowired
    YmsOidGenerator ymsOidGenerator;

    @Autowired
    ReWriteBusCorrDataService reWriteBusCorrDataService;


    @Autowired
    private CtmThreadPoolExecutor ctmThreadPoolExecutor;

    @Autowired
    private AsyncCorrService asyncCorrService;

    /**
     * 执行自动关联调度任务
     * @param corrDataEntity
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation= Propagation.REQUIRES_NEW)
    public void runCorrTask(CorrDataEntity corrDataEntity,int ordernum) throws Exception {
        try {
            //写入关联表
            corrOpration(corrDataEntity,ordernum);
            //回写银行对账单
            reWriteBusCorrDataService.reWriteBankReconciliationData(corrDataEntity, true);
        } catch (Exception e) {
            log.error("runCorrTask:exception", e);
            throw e;
        }
    }

    /**
     * 业务关联操作
     *
     * @param corrData
     * @throws Exception
     */
    @Override
    public BankReconciliationbusrelation_b corrOpration(CorrDataEntity corrData,int ordernum) throws Exception {
        List<String> bankReconciliationIdList = AppContext.cache().getObject("autoAssociationTask-" + AppContext.getYTenantId());
        //20260415增加银行流水关联货币兑换，增加对应判断逻辑
        if(CollectionUtils.isNotEmpty(bankReconciliationIdList)){
            bankReconciliationIdList = AppContext.cache().getObject("autoCorrCurrencyExchangeTask-" + AppContext.getYTenantId());
        }
        if (!corrData.getAuto() && CollectionUtils.isNotEmpty(bankReconciliationIdList) && bankReconciliationIdList.contains(corrData.getBankReconciliationId())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103037"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_211190D404680009","当前流水正在执行自动关联中，请稍后再尝试操作手动关联") /* "当前流水正在执行自动关联中，请稍后再尝试操作手动关联" */);
        }
        BankReconciliationbusrelation_b bankReconciliationbusrelationB = new BankReconciliationbusrelation_b();
        if (corrData.isApi()) {
            //添加数据
            bankReconciliationbusrelationB = setBankData4Api(bankReconciliationbusrelationB, corrData);
            bankReconciliationbusrelationB.setRelationtype(Relationtype.HeterogeneousAssociated.getValue());
            bankReconciliationbusrelationB.setCreateTime(new Date());
            bankReconciliationbusrelationB.setId(ymsOidGenerator.nextId());
            if (corrData.getExtendFields() !=null && corrData.getExtendFields().containsKey("isPartClaim")){
                bankReconciliationbusrelationB.setOrdernum(corrData.getExtendFields().get("claimId") + "");
            }else {
                bankReconciliationbusrelationB.setOrdernum(String.valueOf(ordernum));
            }
            bankReconciliationbusrelationB.setEntityStatus(EntityStatus.Insert);
            //CmpMetaDaoHelper.insert(BankReconciliationbusrelation_b.ENTITY_NAME, bankReconciliationbusrelationB);
            List<BankReconciliationbusrelation_b> relationList = new ArrayList<>();
            relationList.add(bankReconciliationbusrelationB);
            CommonSaveUtils.insertBankReconciliationbusrelation_b(relationList);
            return null;
        }
        boolean updateflag = false;
        /**
         * 生单关联，需判断是否是业务单据编辑保存，如是业务单据编辑保存，执行Update
         */
        if (!corrData.getAuto()) {
            QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
            QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("bankreconciliation").eq(corrData.getBankReconciliationId()));
            querySchema1.addCondition(group1);
            List<BankReconciliationbusrelation_b> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema1, null);
            String status = corrData.getStatus() + "";
            if (bankReconciliations != null && bankReconciliations.size() > 0 && "Update".equals(status)) {
                /**
                 * 业务单据修改触发以下代码，找出对应业务单据的关联关系，进行修改。
                 */
                for (BankReconciliationbusrelation_b b : bankReconciliations) {
                    if (b.getBillcode().equals(corrData.getCode())) {
                        bankReconciliationbusrelationB = b;
                        break;
                    }
                }
                updateflag = true;
            }
        }
        //赋值关联类型
        Short isAuto;
        if (corrData.isGenerate()) {
            isAuto = 2;
        } else {
            isAuto = corrData.getAuto() ? auto : mau;
        }
        //添加数据
        bankReconciliationbusrelationB = setBankData(bankReconciliationbusrelationB, corrData);
        if (corrData.getExtendFields() !=null && corrData.getExtendFields().containsKey("isPartClaim")){
            bankReconciliationbusrelationB.setOrdernum(corrData.getExtendFields().get("claimId") + "");
        }else {
            bankReconciliationbusrelationB.setOrdernum(String.valueOf(ordernum));
        }
        if (updateflag) {
            bankReconciliationbusrelationB.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(BankReconciliationbusrelation_b.ENTITY_NAME, bankReconciliationbusrelationB);
        } else {
            bankReconciliationbusrelationB.setRelationtype(isAuto);
            bankReconciliationbusrelationB.setId(ymsOidGenerator.nextId());
            bankReconciliationbusrelationB.setEntityStatus(EntityStatus.Insert);
            //CmpMetaDaoHelper.insert(BankReconciliationbusrelation_b.ENTITY_NAME, bankReconciliationbusrelationB);
            List<BankReconciliationbusrelation_b> relationList = new ArrayList<>();
            relationList.add(bankReconciliationbusrelationB);
            CommonSaveUtils.insertBankReconciliationbusrelation_b(relationList);
        }
        return bankReconciliationbusrelationB;
    }


    /**
     * 自动关联确认操作
     *
     * @param corrIds @return
     * @return
     */
    @Override
    @Transactional
    public Map<String, Object> confirmCorrOpration(List corrIds, List dcFlags) {
        Map<String, Object> mapres = new HashMap<String, Object>();
        int def = 0;
        int succ = 0;

        for (int i = 0; i < corrIds.size(); i++) {
            int succe = confirm(Long.valueOf(corrIds.get(i).toString()), Long.valueOf(dcFlags.get(i).toString()));
            if (succe == 1) { // 1 为确认失败
                def += 1;
            } else {
                succ += 1;
            }
        }
        mapres.put("succ", succ);
        mapres.put("def", def);
        return mapres;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void asyncConfirmCorrOpration(List corrIds, List dcFlags,String uid) {
        for (int i = 0; i < corrIds.size(); i++) {
            try {
                int rtn = confirmUseExeception(Long.valueOf(corrIds.get(i).toString()), Long.valueOf(dcFlags.get(i).toString()), true);
                if (rtn == 1) {
                    ProcessUtil.addMessage(uid, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C00052", "当前数据已经被确认!") /* "当前数据已经被确认!" */);
                } else {
                    ProcessUtil.refreshProcess(uid, true, 1);
                }
            } catch (Exception e) {
                log.error("asyncConfirmCorrOpration:exception", e);
                ProcessUtil.addMessage(uid, e.getMessage());
            }
        }
    }

    /**
     * 自动关联确认操作
     *
     * @param corrIds @return
     * @return
     */
    @Override
    @Transactional
    public Map<String, Object> confirmCorrOprationUseException(List corrIds, List dcFlags) throws Exception {
        Map<String, Object> mapres = new HashMap<String, Object>();
        int def = 0;
        int succ = 0;

        for (int i = 0; i < corrIds.size(); i++) {
            int succe = confirmUseException(Long.valueOf(corrIds.get(i).toString()), Long.valueOf(dcFlags.get(i).toString()));
            if (succe == 1) { // 1 为确认失败
                def += 1;
            } else {
                succ += 1;
            }
        }
        mapres.put("succ", succ);
        mapres.put("def", def);
        return mapres;
    }

    /**
     * return 1;  拒绝失败；
     * return 0；拒绝成功；
     *
     * @param corrIds@return
     * @throws Exception
     */
    @Override
    public Map<String, Object> refuseCorrOpration(List corrIds) {
        Map<String, Object> mapres = new HashMap<String, Object>();
        int succ = 0;
        int def = 0;

        for (int i = 0; i < corrIds.size(); i++) {
            int succe = refconfirm(Long.valueOf(corrIds.get(i).toString()), false);
            if (succe == 1) {
                def += 1;
            } else {
                succ += 1;
            }
        }

        mapres.put("succ", succ);
        mapres.put("def", def);
        return mapres;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void asyncRefuseCorrOpration(List corrIds,String uid) {
        for (int i = 0; i < corrIds.size(); i++) {
            try {
                int succe = refconfirm(Long.valueOf(corrIds.get(i).toString()), true);
                if (succe == 1) {
                    ProcessUtil.addMessage(uid, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C00052", "当前数据已经被确认!") /* "当前数据已经被确认!" */);
                } else {
                    ProcessUtil.refreshProcess(uid, true, 1);
                }
            } catch (Exception e) {
                log.error("asyncRefuseCorrOpration:exception", e);
                ProcessUtil.addMessage(uid, e.getMessage());
            }
        }
    }

    /**
     * 回写银行流水认领关联数据
     * 同名账户划转: 银行转账单,缴存现金单（收款流水）,提取现金单（付款流水）,第三方转账
     *
     * @param corrData
     * @throws Exception
     */
    @Override
    public void reWriteTransferAccCorrelationOperation(CorrDataEntity corrData) throws Exception {
//        if (null != corrData.getBillClaimItemId()) {
//            //认领单
//            BankReconciliationbusrelation_b subBody = new BankReconciliationbusrelation_b();
//            this.setBankData(subBody, corrData);
//            subBody.setRelationtype(Short.valueOf("2"));
//            subBody.setId(ymsOidGenerator.nextId());
//            subBody.setEntityStatus(EntityStatus.Insert);
//            CmpMetaDaoHelper.insert(BankReconciliationbusrelation_b.ENTITY_NAME, subBody);
//        } else {
        //新增操作
        BankReconciliationbusrelation_b subBody = new BankReconciliationbusrelation_b();
        this.setBankData(subBody, corrData);
        subBody.setRelationtype(Short.valueOf("2"));
        subBody.setId(ymsOidGenerator.nextId());
        subBody.setEntityStatus(EntityStatus.Insert);
        //CmpMetaDaoHelper.insert(BankReconciliationbusrelation_b.ENTITY_NAME, subBody);
        List<BankReconciliationbusrelation_b> relationList = new ArrayList<>();
        relationList.add(subBody);
        CommonSaveUtils.insertBankReconciliationbusrelation_b(relationList);
//        }
    }

    /**
     * 关联确认 拒绝
     *
     * @param corrId
     * @param isBatch true 批量操作;false 单条操作
     * @return
     */
    @Transactional
    public int refconfirm(Long corrId, boolean isBatch) {
        try {
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(corrId));
            group.appendCondition(QueryCondition.name("relationstatus").eq(Relationstatus.Confirm.getValue()));
            querySchema.addCondition(group);
            Long bid = 0L;
            List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema, null);
            List<BankReconciliationbusrelation_b> checkbankReconciliationbusrelation_bs = null;
            boolean  flagMark = false;
            if (CollectionUtils.isNotEmpty(bankReconciliationbusrelation_bs)) {
                bid = bankReconciliationbusrelation_bs.get(0).getBankreconciliation();
                BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, bid, 3);
                if (bankReconciliation.BankReconciliationbusrelation_b() == null || bankReconciliation.BankReconciliationbusrelation_b().size() <=1){
                    flagMark = true;
                }
                if (isBatch) {
                    checkbankReconciliationbusrelation_bs = bankReconciliation.BankReconciliationbusrelation_b().stream()
                            .filter(item -> item.getBilltype() == EventType.StwbSettleMentDetails.getValue()).collect(Collectors.toList());
                    boolean flag = checkbankReconciliationbusrelation_bs.stream().anyMatch(item -> item.getRelationstatus() == 2);
                    if (flag && checkbankReconciliationbusrelation_bs.size() == 1) {
                        return 1;
                    }
                    List<String> ids = bankReconciliationbusrelation_bs.stream().map(item -> item.getId().toString()).collect(Collectors.toList());
                    checkbankReconciliationbusrelation_bs = checkbankReconciliationbusrelation_bs.stream().filter(item -> !ids.contains(item.getId().toString())).collect(Collectors.toList());
                    if (CollectionUtils.isNotEmpty(checkbankReconciliationbusrelation_bs)){
                        MetaDaoHelper.delete(BankReconciliationbusrelation_b.ENTITY_NAME, checkbankReconciliationbusrelation_bs);
                    }
                } else {
                    if (bankReconciliationbusrelation_bs.get(0).getRelationstatus() == 2) {
                        return 1;
                    }
                }
                //MetaDaoHelper.delete(BankReconciliationbusrelation_b.ENTITY_NAME, bankReconciliationbusrelation_bs);
            }
            if (bid != 0) {
                QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("id").eq(bid));
                querySchema1.addCondition(group1);
                List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema1, null);
                if (bankReconciliations != null && bankReconciliations.size() > 0) {
                    Short associationstatus = 0;
                    //改到CommonSaveUtils中统一操作
                    /*bankReconciliations.get(0).setAssociationstatus(associationstatus);*/
                    bankReconciliations.get(0).setEntityStatus(EntityStatus.Update);
                    //智能到账，拒绝关联时，流程完结状态修改为未完结
                    /*bankReconciliations.get(0).setSerialdealendstate(SerialdealendState.UNEND.getValue());*/
                    if (flagMark){
                        bankReconciliations.get(0).setSerialdealtype(null);
                    }
                    Short processstatus = bankReconciliations.get(0).getProcessstatus();
                    if (DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_PROCESS_FINISH.getStatus().equals(processstatus)) {
                        bankReconciliations.get(0).setProcessstatus(DealDetailEnumConst.DealDetailProcessStatusEnum.DEALDETAIL_MATCH_NO_START.getStatus());
                    }
                    CommonSaveUtils.updateBankReconciliationConfirmOrReject(bankReconciliations.get(0),"2");
                }
            }
            if (CollectionUtils.isNotEmpty(bankReconciliationbusrelation_bs)) {
                CommonSaveUtils.batchDeleteBankReconciliationbusrelation_b(bankReconciliationbusrelation_bs);
            }
        } catch (Exception e) {
            log.error("执行修改报错,错误信息" + e);
            throw new CtmException(e.getMessage());
        }
        return 0;
    }

    public int confirm(Long corrId, Long dcFlag) {
        try {
            confirmUseException(corrId, dcFlag);
        } catch (Exception e) {
            log.error("执行修改报错,错误信息" + e);
            return 1;
        }
        return 0;
    }

    @Override
    public int confirmUseException(Long corrId, Long dcFlag) throws Exception {
        return confirmUseExeception(corrId, dcFlag, true);
    }

    /**
     * @param corrId
     * @param dcFlag
     * @param isBatch
     * @return
     * @throws Exception
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public int confirmUseExeception(Long corrId, Long dcFlag, boolean isBatch) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(corrId));
        querySchema.addCondition(group);
        List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema, null);
        if (bankReconciliationbusrelation_bs != null && bankReconciliationbusrelation_bs.size() > 0) {
            BankReconciliationbusrelation_b bankReconciliationbusrelation_b = bankReconciliationbusrelation_bs.get(0);
            // Relationstatus() == 2  2代表 已确认 1代表 待确认
            if (bankReconciliationbusrelation_b.getRelationstatus() == 2) {
                return 1;
            }
            Short isSure = 2;
            //智能对账：添加智能对账勾兑号
//            String smartcheckno = UUID.randomUUID().toString().replace("-", "");
            //调用资金结算财资统一对账码接口生成
            String smartcheckno = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();
            BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, bankReconciliationbusrelation_bs.get(0).getBankreconciliation());
            if (StringUtils.isEmpty(bankReconciliation.getSmartcheckno())) {
                bankReconciliation.setSmartcheckno(smartcheckno);
            } else {
                smartcheckno = bankReconciliation.getSmartcheckno();
            }
            //退票状态校验：疑似退票
            if (bankReconciliation.getRefundstatus() != null && bankReconciliation.getRefundstatus().equals(refundStatus)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101644"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418059C", "当前银行对账单为疑似退票，请先进行退票确认。") /* "当前银行对账单为疑似退票，请先进行退票确认。" */);
            }
            //select * from aa_enum where enumtype='aa_EventType'
            //资金收款单
            if (17 == bankReconciliationbusrelation_b.getBilltype()) {
                QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("id").eq(bankReconciliationbusrelation_b.getBillid().toString()));
                querySchema1.addCondition(group1);
                List<FundCollection_b> fundCollectionList1 = MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, querySchema1, null);
                if (fundCollectionList1 != null && fundCollectionList1.size() > 0) {
                    // 判断是否已经关联，1 表示 已关联
                    if (fundCollectionList1.get(0).getAssociationStatus() == 1) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101645"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418059B", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                    }
                    fundCollectionList1.get(0).setBankReconciliationId(bankReconciliationbusrelation_b.getBankreconciliation().toString());
                    fundCollectionList1.get(0).setAssociationStatus(corr);
                    //1130认领V3。关联操作将结算状态由结算成功，更改为 已结算补单
                    fundCollectionList1.get(0).setFundSettlestatus(FundSettleStatus.SettlementSupplement);
                    fundCollectionList1.get(0).put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(fundCollectionList1.get(0).getFundSettlestatus()));
                    fundCollectionList1.get(0).setEntityStatus(EntityStatus.Update);
                    //智能对账：资金收款单明细添加智能勾兑码
                    fundCollectionList1.get(0).setSmartcheckno(smartcheckno);
                    MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, fundCollectionList1.get(0));
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101645"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418059B", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            } else if (18 == bankReconciliationbusrelation_b.getBilltype()) { //资金付款单
                QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("id").eq(bankReconciliationbusrelation_b.getBillid().toString()));
                querySchema1.addCondition(group1);
                List<FundPayment_b> fundPaymentList = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, querySchema1, null);
                if (fundPaymentList != null && fundPaymentList.size() > 0) {
                    if (fundPaymentList.get(0).getAssociationStatus() == 1) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101645"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418059B", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                    }
                    fundPaymentList.get(0).setBankReconciliationId(bankReconciliationbusrelation_b.getBankreconciliation().toString());
                    fundPaymentList.get(0).setAssociationStatus(corr);
                    //1130认领V3。关联操作将结算状态由结算成功，更改为 已结算补单
                    fundPaymentList.get(0).setFundSettlestatus(FundSettleStatus.SettlementSupplement);
                    fundPaymentList.get(0).put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(fundPaymentList.get(0).getFundSettlestatus()));
                    fundPaymentList.get(0).setEntityStatus(EntityStatus.Update);
                    //智能对账：资金付款单明细添加智能勾兑码
                    fundPaymentList.get(0).setSmartcheckno(smartcheckno);
                    MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, fundPaymentList.get(0));
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101645"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418059B", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            } else if (12 == bankReconciliationbusrelation_b.getBilltype()) { //转账单
                QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("id").eq(bankReconciliationbusrelation_b.getBillid()));
                querySchema1.addCondition(group1);
                List<TransferAccount> transferAccountList = MetaDaoHelper.queryObject(TransferAccount.ENTITY_NAME, querySchema1, null);
                if (transferAccountList != null && transferAccountList.size() > 0) {
                    if (dcFlag == 1) { //借 ：付
                        if (transferAccountList.get(0).getAssociationStatusPay()) { //付款关联状态 布尔类型
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101645"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418059B", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                        }
                        //设置付款_银行对账单id
                        transferAccountList.get(0).setPaybankbill(bankReconciliationbusrelation_b.getBankreconciliation());
                        //设置付款是否关联标识
                        transferAccountList.get(0).setAssociationStatusPay(true);
                        transferAccountList.get(0).setEntityStatus(EntityStatus.Update);
                        transferAccountList.get(0).setPaysmartcheckno(smartcheckno);
                        MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccountList.get(0));
                    } else { //贷 ：收
                        if (transferAccountList.get(0).getAssociationStatusCollect()) {  //收款关联状态 布尔类型
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101645"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418059B", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                        }
                        //设置收款_银行对账单id
                        transferAccountList.get(0).setCollectbankbill(bankReconciliationbusrelation_b.getBankreconciliation());
                        //设置收款是否关联标识
                        transferAccountList.get(0).setAssociationStatusCollect(true);
                        transferAccountList.get(0).setEntityStatus(EntityStatus.Update);
                        transferAccountList.get(0).setSmartcheckno(smartcheckno);
                        MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccountList.get(0));
                    }
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101645"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418059B", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }

            } else if (14 == bankReconciliationbusrelation_b.getBilltype()) {//外币兑换单
                QuerySchema querySchema1 = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM);
                QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("id").eq(bankReconciliationbusrelation_b.getBillid()));
                querySchema1.addCondition(group1);
                //根据银行对账单子表的业务单据id 查询到对应的外币兑换单
                List<CurrencyExchange> currencyExchangeList = MetaDaoHelper.queryObject(CurrencyExchange.ENTITY_NAME, querySchema1, null);
                //对查询到的结果进行判空
                if (currencyExchangeList != null && currencyExchangeList.size() > 0) {
                    //判断借贷方向，1为借 ：付
                    if (dcFlag == 1) {
                        Short associationStatusPay = currencyExchangeList.get(0).getAssociationStatusPay();
                        //判断付款是否关联状态 1关联0未关联
                        if (associationStatusPay == 1) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101646"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0021E", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                        }
                        //设置付款_银行对账单id
                        currencyExchangeList.get(0).setPaybankbill(bankReconciliationbusrelation_b.getBankreconciliation());
                        //设置付款是否关联标识
                        currencyExchangeList.get(0).setAssociationStatusPay((short) 1);
                        currencyExchangeList.get(0).setEntityStatus(EntityStatus.Update);
                        currencyExchangeList.get(0).setSellsmartcheckno(smartcheckno);
                        //更新转账单
                        MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, currencyExchangeList.get(0));
                    } else { //借贷方向为 贷 ：收
                        Short associationStatusCollect = currencyExchangeList.get(0).getAssociationStatusCollect();
                        //判断收款是否关联状态  1关联0未关联
                        if (associationStatusCollect == 1) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101646"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0021E", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                        }
                        //设置收款_银行对账单id
                        currencyExchangeList.get(0).setCollectbankbill(bankReconciliationbusrelation_b.getBankreconciliation());
                        //设置收款是否关联标识
                        currencyExchangeList.get(0).setAssociationStatusCollect((short) 1);
                        currencyExchangeList.get(0).setEntityStatus(EntityStatus.Update);
                        currencyExchangeList.get(0).setBuysmartcheckno(smartcheckno);
                        //更新转账单
                        MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, currencyExchangeList.get(0));
                    }
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101646"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0021E", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                }
            } else if (EventType.StwbSettleMentDetails.getValue() == bankReconciliationbusrelation_b.getBilltype()) {//资金结算
                SettleDeatailRelBankBillReqVO json = new SettleDeatailRelBankBillReqVO();
                Date tranTime = bankReconciliation.getTran_time() == null ? bankReconciliation.getTran_date() : bankReconciliation.getTran_time();

                //查询银行对账单关联的子表数据
                BankReconciliation mainData = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, bankReconciliationbusrelation_bs.get(0).getBankreconciliation(), 3);
                List<BankReconciliationbusrelation_b> relationList = mainData.BankReconciliationbusrelation_b().stream().filter(item -> item.getBilltype() == EventType.StwbSettleMentDetails.getValue()).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(relationList)) {
                    return 1;
                }
                //
                if (relationList.stream().anyMatch(bs -> bs.getBilltype() == 60 && bs.getRelationstatus() == 2)) {
                    //有关联的则移除
                    List<BankReconciliationbusrelation_b> deleteList = relationList.stream().filter(bs -> bs.getBilltype() == 60 && bs.getRelationstatus() != 2).collect(Collectors.toList());
                    if (deleteList != null){
                        MetaDaoHelper.delete(BankReconciliationbusrelation_b.ENTITY_NAME, deleteList);
                    }
                    return 0;
                }
                //批量操作
                if (isBatch) {
                    if (CollectionUtils.isNotEmpty(relationList) && relationList.size() > 1) {
                        //如果对账单关联了多个资金结算明细，则需要找对账单业务单据关联表中与对账单中交易日期最接近的那条数据
                        bankReconciliationbusrelation_b = querySettleData(bankReconciliation.getId(), tranTime, relationList);
                        if (bankReconciliationbusrelation_b != null){
                            Long id = bankReconciliationbusrelation_b.getId();
                            List<BankReconciliationbusrelation_b> deleteList = relationList.stream().filter(bs -> !bs.getId().equals(id)).collect(Collectors.toList());
                            if (deleteList != null){
                                MetaDaoHelper.delete(BankReconciliationbusrelation_b.ENTITY_NAME, deleteList);
                            }
                        }
                    }
                }else {
                    //移除批量操作的其他关联的结算单关联关系
                    if (CollectionUtils.isNotEmpty(relationList) && relationList.size() > 1) {
                        List<BankReconciliationbusrelation_b> deleteList = relationList.stream().filter(bs -> !bs.getId().equals(corrId)).collect(Collectors.toList());
                        if (deleteList != null){
                            MetaDaoHelper.delete(BankReconciliationbusrelation_b.ENTITY_NAME, deleteList);
                        }
                    }
                }
                //根据结算明细的id检查是否在多个银行流水下存在关联，如果有，则将其删除，更新非当前对账单的其他对账单的状态  (其他对账单如果只有一条关联关系，且该条数据被删除，则将其修改为未关联状态)
                if (bankReconciliationbusrelation_b != null) {
                    deleteOtherBusRelationData(bankReconciliationbusrelation_b, relationList);
                    json.setBankCheck_id(bankReconciliationbusrelation_b.getBankreconciliation().toString());
                    json.setSettleBenchB_id(bankReconciliationbusrelation_b.getBillid().toString());
                }

                //智能对账：新增勾兑号传递
                json.setCheck_identification_code(smartcheckno);
                //1为退票
                short isrefund_status = 1;
                //如果退票状态不为空，并且为退票
                //Date tranTime = bankReconciliation.getTran_time() == null ? bankReconciliation.getTran_date() : bankReconciliation.getTran_time();
                SimpleDateFormat outputFormat = new SimpleDateFormat(DateUtils.DATE_TIME_PATTERN);
                if (bankReconciliation.getRefundstatus() != null && bankReconciliation.getRefundstatus().equals(RefundStatus.Refunded.getValue())) {
                    //传入退票状态和退票金额
                    json.setIsrefund(String.valueOf(isrefund_status));
                    json.setRefundAmt(bankReconciliation.getTran_amt());
                } else { //非退票.必传，不然结算会报错
                    json.setIsrefund(String.valueOf(0));
                    json.setRefundAmt(new BigDecimal(String.valueOf(0)));
                }
                if (null != bankReconciliation.getTran_time()) {
                    json.setRefundDate(bankReconciliation.getTran_time());//退票时间
                } else {
                    String formattedDateTime = outputFormat.format(bankReconciliation.getTran_date());
                    // 将日期字符串解析为 Date 对象，带有时分秒
                    json.setRefundDate(outputFormat.parse(formattedDateTime));//退票时间
                }
                // 将日期字符串解析为 Date 对象，带有时分秒
                //CZFW-273088 关联接口传递交易时间
                json.setSettlesuccesstime(outputFormat.format(tranTime));
                //CZFW-273088 关联接口传递交易日期
                json.setSettlesuccessdate(DateUtils.dateFormat(tranTime, DateUtils.DATE_TIME_PATTERN));
                //关联确认接口增加金额传递
                json.setActualExchangePaymentAmount(bankReconciliation.getTran_amt());
                //财资统一对账码处理
                try {
                    ResultStrRespVO respVO = RemoteDubbo.get(SettleBenchBRPCService.class, IDomainConstant.MDD_DOMAIN_STWB).newRelationSettleBench(json);
                    if (!org.apache.commons.lang3.StringUtils.isEmpty(respVO.getCheckIdentificationCode())) {
                        bankReconciliation.setSmartcheckno(respVO.getCheckIdentificationCode());
                    }
                    //智能流水-关联确认的时候给未确认的银行流水赋值
                    if (ConfirmStatusEnum.Confirming.getIndex().equals(bankReconciliation.getConfirmstatus())){
                        bankReconciliation.setAccentity(respVO.getAccentity());
                        bankReconciliation.setConfirmstatus(ConfirmStatusEnum.RelationConfirmed.getIndex());
                        //1客户；2供应商；3员员工
                        if ("1".equals(respVO.getCounterpartyType()) || "2".equals(respVO.getCounterpartyType()) || "3".equals(respVO.getCounterpartyType())){
                            bankReconciliation.setOppositetype(Short.valueOf(respVO.getCounterpartyType()));
                            bankReconciliation.setOppositeobjectid(respVO.getCounterpartyId());
                            bankReconciliation.setOppositeobjectname(respVO.getCounterpartyName());
                        }else {
                            bankReconciliation.setOppositetype(OppositeType.Other.getValue());
                            bankReconciliation.setOppositeobjectid(null);
                            bankReconciliation.setOppositeobjectname(null);
                        }
                    }
                }catch (Exception e){
                    log.error("SettleBenchBRPCService newRelationSettleBench错误，请求参数 = {},报错信息={}",json,e.getMessage());
                    throw e;
                }
            }
            //更新对账单
            EntityTool.setUpdateStatus(bankReconciliation);
            CommonSaveUtils.updateBankReconciliationConfirmOrReject(bankReconciliation,"1");
            //更新关联明细
            if (bankReconciliationbusrelation_b != null){
                bankReconciliationbusrelation_b.setRelationstatus(isSure);
                bankReconciliationbusrelation_b.setOrdernum("1");
                bankReconciliationbusrelation_b.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(BankReconciliationbusrelation_b.ENTITY_NAME, bankReconciliationbusrelation_b);
            }
        }
        return 0;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public CtmJSONObject progess(CtmJSONObject params) {
        TransactionSynchronizationManager.getCurrentTransactionName();
        CtmJSONObject responseMsg = new CtmJSONObject();
        List corrIds = (List) params.get("corrId");
        if (CollectionUtils.isEmpty(corrIds)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101750"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_MDD-BACK_189A3F320450003A", "请至少选择一条数据！") /* "请至少勾选一条数据！" */);
        }
        String uid = params.getString("uid");
        String status = params.getString("status");
        int listSize = corrIds.size();
        //构建进度条信息
        ProcessUtil.initProcess(uid, listSize);
        ctmThreadPoolExecutor.getThreadPoolExecutor().submit(() -> {
            try {
                if ("sure".equals(status)) {
                    List dcFlags = (List) params.get("dc_flag");
                    asyncCorrService.asyncConfirmCorrOpration(corrIds,dcFlags, uid);
                } else if ("refuse".equals(status)) {
                    asyncCorrService.asyncRefuseCorrOpration(corrIds, uid);
                }
                //ProcessUtil.completedResetCount(uid);
            } catch (Exception e) {
                log.error("changeRepeatStatus", e);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101751"), e.getMessage());
            } finally {
                ProcessUtil.completedResetCount(uid);
            }
        });
        responseMsg.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C00053", "更新疑重状态开始") /* "更新疑重状态开始" */);
        return responseMsg;
    }


    public BankReconciliationbusrelation_b setBankData(BankReconciliationbusrelation_b b, CorrDataEntity corrData) {
        //赋值业务单据
        short billtype;
        if (corrData.getBillType().equals(BussDocumentType.fundcollection.getValue() + "")) {
            billtype = EventType.FundCollection.getValue();
        } else if (corrData.getBillType().equals(BussDocumentType.fundpayment.getValue() + "")) {
            billtype = EventType.FundPayment.getValue();
        } else if (corrData.getBillType().equals(BussDocumentType.transferaccount.getValue() + "")) {
            billtype = EventType.TransferAccount.getValue();
        } else if (corrData.getBillType().equals(BussDocumentType.currencyexchange.getValue() + "")) {
            billtype = EventType.CurrencyExchangeBill.getValue();
        } else if (corrData.getBillType().equals(BussDocumentType.Payment.getValue() + "")) {
            billtype = EventType.PayMent.getValue();
        } else if (corrData.getBillType().equals(BussDocumentType.ReceiveBill.getValue() + "")) {
            billtype = EventType.ReceiveBill.getValue();
        } else if (tlm_repayment.equals(corrData.getBillType())) {//融资还本
            billtype = EventType.Financingrepayment.getValue();
        } else if (tlm_payinterest.equals(corrData.getBillType())) {//融资付息
            billtype = EventType.tlm_payinterest.getValue();
        } else if (tlm_financepayfee.equals(corrData.getBillType())) {//融资付费
            billtype = EventType.tlm_financepayfee.getValue();
        } else if (tlm_financeinStatement.equals(corrData.getBillType())) {//融入登记
            billtype = EventType.LendingRegistration.getValue();
        } else if (tlm_interestcollection.equals(corrData.getBillType())) {//投资收息单
            billtype = EventType.tlm_interestcollection.getValue();
        } else if (tlm_purchaseregister.equals(corrData.getBillType())) {//申购登记单
            billtype = EventType.tlm_purchaseregister.getValue();
        } else if (tlm_investredem.equals(corrData.getBillType())) {//投资赎回单
            billtype = EventType.tlm_investredem.getValue();
        } else if (tlm_investpayment.equals(corrData.getBillType())) {//投资付费单
            billtype = EventType.tlm_investpayment.getValue();
        } else if (tlm_investprofitsharing.equals(corrData.getBillType())) {//投资分红单
            billtype = EventType.tlm_investprofitsharing.getValue();
        } else if (tlm_tradedelivery.equals(corrData.getBillType())) {//衍生品交易交割单
            billtype = EventType.tlm_tradedelivery.getValue();
        } else if (tlm_derivativesclose.equals(corrData.getBillType())) {//衍生品交易平仓单
            billtype = EventType.tlm_derivativesclose.getValue();
        } else if (tlm_traderolloverregister.equals(corrData.getBillType())) {//衍生品交易展期登记单
            billtype = EventType.tlm_traderolloverregister.getValue();
        } else if (tlm_addbond.equals(corrData.getBillType())) {//衍生品交易保证金登记单
            billtype = EventType.tlm_addbond.getValue();
        } else if ((EventType.CurrencyExchangeBill.getValue() + "").equals(corrData.getBillType())) {//外币兑换单
            billtype = EventType.CurrencyExchangeBill.getValue();
        } else {
            billtype = EventType.StwbSettleMentDetails.getValue();
        }
        Short isSur = corrData.getAuto() ? Relationstatus.Confirm.getValue() : Relationstatus.Confirmed.getValue();
        b.setBankreconciliation(corrData.getBankReconciliationId());
        //外币兑换工作台关联corrData的主键为busid
        if (billtype == EventType.TransferAccount.getValue() || billtype == EventType.CurrencyExchangeBill.getValue()) {
            b.setSrcbillid(corrData.getBusid());
        } else {
            b.setSrcbillid(corrData.getMainid());
        }
        b.setBillid(corrData.getBusid());
        b.setVouchdate(corrData.getVouchdate());
        b.setBillcode(corrData.getCode());
        b.setAmountmoney(corrData.getOriSum());
        b.setAccentity(corrData.getAccentity());
        b.setBillnum(corrData.getBillNum());
        b.setBilltype(billtype);
        b.setDept(corrData.getDept());
        b.setProject(corrData.getProject());
        b.setCreateTime(new Date());
        // CZFW-368598，自动生单自动提交的，为自动生单
        if (corrData.getAuto() && corrData.getExtendFields() != null && corrData.getExtendFields().containsKey("isAutoSubimit")) {
            b.setRelationstatus(Relationstatus.Confirmed.getValue());
        } else {
            b.setRelationstatus(isSur);
        }
        b.setIsadvanceaccounts(corrData.getIsadvanceaccounts());
        b.setAssociationcount(corrData.getAssociationcount());
        return b;
    }

    /**
     * * 为银行对账单的子表添加数据
     *
     * @param b
     * @param corrData
     * @return
     */
    public BankReconciliationbusrelation_b setBankData4Api(BankReconciliationbusrelation_b b, CorrDataEntity corrData) {
        //赋值业务单据
       // Short isSur = corrData.getAuto() ? Relationstatus.Confirm.getValue() : Relationstatus.Confirmed.getValue();
        b.setBankreconciliation(corrData.getBankReconciliationId());
        //主表id
        b.setSrcbillid(corrData.getMainid());
        //子表id
        b.setBillid(corrData.getBusid());
        b.setVouchdate(corrData.getVouchdate());
        b.setBillcode(corrData.getCode());
        b.setAmountmoney(corrData.getOriSum());
        b.setAccentity(corrData.getAccentity());
        b.setBillnum(corrData.getBillNum());
        if (!StringUtils.isEmpty(corrData.getBillType())) {
            b.setBilltype(Short.parseShort(corrData.getBillType()));
        }
        b.setOutbilltypename(corrData.getOutBillTypeName());
        b.setDept(corrData.getDept());
        b.setProject(corrData.getProject());
       // b.setOrdernum(corrData.getOrdernum());
        //关联状态
        b.setRelationstatus(Relationstatus.Confirmed.getValue());
        //特征
        if (corrData.getCharacterDef() != null) {
            b.put("characterDef", corrData.getCharacterDef());
        }
        //扩展字段
        if (corrData.getExtendFields() != null) {
            Set<Map.Entry<String, Object>> paramEntries = corrData.getExtendFields().entrySet();
            for (Map.Entry<String, Object> paramEntry : paramEntries) {
                b.put(paramEntry.getKey(), paramEntry.getValue());
            }
        }
        return b;
    }

    /**
     * 根据结算明细的id检查是否在多个银行流水下存在关联，如果有，则将其删除，更新非当前对账单的其他对账单的状态  (其他对账单如果只有一条关联关系，且该条数据被删除，则将其修改为未关联状态)
     *
     * @param bankReconciliationbusrelationB
     * @throws Exception
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void deleteOtherBusRelationData(BankReconciliationbusrelation_b bankReconciliationbusrelationB, List<BankReconciliationbusrelation_b> relationList) throws Exception {
        List<Long> unrelationIds = new ArrayList<>();
        List<Long> deleteBsIds = new ArrayList<>();
        Long settleBillIds = bankReconciliationbusrelationB.getBillid();
        QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM)
                .appendQueryCondition(QueryCondition.name("billid").eq(settleBillIds));
        //通过结算单查询所有的银行流水子表
        List<BankReconciliationbusrelation_b> list = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isNotEmpty(list)) {
            //将确认的银行流水移除掉
            Map<Long, List<BankReconciliationbusrelation_b>> map = list.stream().filter(item -> !item.getBankreconciliation().equals(bankReconciliationbusrelationB.getBankreconciliation()))
                    .collect(Collectors.groupingBy(BankReconciliationbusrelation_b::getBankreconciliation));
            //获取涉及的银行流水的id集合
            List<Long> banIds = list.stream().filter(item -> !item.getBankreconciliation().equals(bankReconciliationbusrelationB.getBankreconciliation())).map(BankReconciliationbusrelation_b::getBankreconciliation).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(banIds)) {
                //通过集合查询对应银行流水的子表数据
                QuerySchema querySchemaChild = QuerySchema.create().addSelect(ICmpConstant.SELECT_TOTAL_PARAM)
                        .appendQueryCondition(QueryCondition.name("bankreconciliation").in(banIds));
                //获取需要移除数据的银行对账单的子表数据，为了判断要移除的其他的银行对账单是否存在一对多的情况
                List<BankReconciliationbusrelation_b> listAll = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchemaChild, null);
                if (CollectionUtils.isNotEmpty(listAll)) {
                    //对获取到的相关结算单涉及的所有银行流水通过银行流水的id进行分组，用于判断移除后是否还有关联的其他结算单，按照银行对账单进行分组，为了判断是否修改主表状态
                    Map<Long, List<BankReconciliationbusrelation_b>> allMap = listAll.stream().collect(Collectors.groupingBy(BankReconciliationbusrelation_b::getBankreconciliation));
                    //如果银行对账单只绑定了一个结算明细，则需要解除关联关系；如果关联了多个结算明细，只需要将该结算明细删除
                    map.forEach((key, value) -> {
                        //获取到关联的银行流水，若存在多个，则只移除子表，若存在一个，则需要移除主表信息
                        List<BankReconciliationbusrelation_b> bankReconciliationbusrelationBs = allMap.get(key);
                        if (CollectionUtils.isNotEmpty(bankReconciliationbusrelationBs)) {
                            if (bankReconciliationbusrelationBs.size() > 1) {
                                deleteBsIds.addAll(value.stream().filter(item->!item.getId().equals(bankReconciliationbusrelationB.getId())).map(item -> item.getLong("id")).collect(Collectors.toList()));
                            } else if (bankReconciliationbusrelationBs.size() == 1) {
                                if (!value.get(0).getLong("id").equals(bankReconciliationbusrelationB.getId())){
                                    deleteBsIds.add(value.get(0).getLong("id"));
                                    unrelationIds.add(key);
                                }
                            }
                        }
                    });
                }
            }
        }
        if (CollectionUtils.isNotEmpty(unrelationIds)) {
            for (Long id : unrelationIds) {
                BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, id,3);
                //改到CommonSaveUtils中统一操作
                /*bankReconciliation.setAssociationstatus((short)0);*/
                bankReconciliation.setEntityStatus(EntityStatus.Update);
                //智能到账，拒绝关联时，流程完结状态修改为未完结
                /*bankReconciliation.setSerialdealendstate(SerialdealendState.UNEND.getValue());*/
                if (bankReconciliation.BankReconciliationbusrelation_b() == null || bankReconciliation.BankReconciliationbusrelation_b().size() <=1){
                    bankReconciliation.setSerialdealtype(null);
                }
                CommonSaveUtils.updateBankReconciliationConfirmOrReject(bankReconciliation,"2");
            }
        }
        if (CollectionUtils.isNotEmpty(deleteBsIds)) {
            List<BankReconciliationbusrelation_b> deleteBizObj = list.stream().filter(item -> deleteBsIds.contains(item.getLong("id"))).collect(Collectors.toList());
            //MetaDaoHelper.delete(BankReconciliationbusrelation_b.ENTITY_NAME,deleteBizObj);
            CommonSaveUtils.batchDeleteBankReconciliationbusrelation_b(deleteBizObj);
        }
    }

    /**
     * 根据银行流水关联表中的billid字段查询结算明细
     * 结算明细有两个字段。如果结算中的取期望结算日期，如果是结算成功或部分成功取结算成功日期
     * //{\"8\":\"已退回\",\"7\":\"部分成功\",\"1\":\"待结算\",\"2\":\"结算中\",\"3\":\"结算成功\",\"4\":\"结算失败\",\"6\":\"已止付\"}
     *
     * @param id
     * @param tranTime
     * @param bankReconciliationbusrelation_bs
     * @return
     * @throws Exception
     */
    private BankReconciliationbusrelation_b querySettleData(Long id, Date tranTime, List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs) throws Exception {
        List<Long> billIds = bankReconciliationbusrelation_bs.stream().map(BankReconciliationbusrelation_b::getBillid).collect(Collectors.toList());
        //
        QuerySchema settleBenchSchema = QuerySchema.create().addSelect("id,statementdetailstatus,expectdate,settlesuccessdate")
                .appendQueryCondition(QueryCondition.name("id").in(billIds), QueryCondition.name("enabledStatus").eq(1));
        List<SettleBench_b> settleBenchList = MetaDaoHelper.queryObject(SettleBench_b.ENTITY_NAME, settleBenchSchema, null);
        //结算明细有两个字段。如果结算中的取期望结算日期，如果是结算成功或部分成功取结算成功日期
        Optional<SettleBench_b> settleBenchOptional = settleBenchList.stream().filter(item -> Arrays.asList(2, 3, 7).contains(item.getStatementdetailstatus())).min(Comparator.comparing(item -> {
            if (item.getStatementdetailstatus() == 2) {
                return Math.abs(item.getDate("expectdate").getTime() - tranTime.getTime());
            } else {
                return Math.abs(item.getDate("settlesuccessdate").getTime() - tranTime.getTime());
            }
        }));
        if (settleBenchOptional.isPresent()) {
            SettleBench_b settleBench_b = settleBenchOptional.get();
            Long settleBenchBId = settleBench_b.getId();
            Optional<BankReconciliationbusrelation_b> optional = bankReconciliationbusrelation_bs.stream().filter(item -> settleBenchBId.equals(item.getBillid())).findFirst();
            if (optional.isPresent()) {
                return optional.get();
            }
        }
        return null;
    }
}
