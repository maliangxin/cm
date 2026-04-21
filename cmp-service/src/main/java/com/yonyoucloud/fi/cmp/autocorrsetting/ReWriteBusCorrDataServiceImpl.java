package com.yonyoucloud.fi.cmp.autocorrsetting;


import com.yonyou.cloud.utils.StringUtils;
import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.CtmAppContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyoucloud.ctm.stwb.reconcode.pubitf.ReconciliateCodeGenerator;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankPublishSendMsgService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.enums.SerialdealendState;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.CorrDataEntity;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 业务关联回写单据信息
 *
 * @author msc
 */
@Service
@Slf4j
public class ReWriteBusCorrDataServiceImpl implements ReWriteBusCorrDataService {

    private final String AUTOCORRSETTINGLISTMAPPER = "com.yonyoucloud.fi.cmp.mapper.AutoCorrSettingMapper.";

    private final Short delcorrStatus = 0;
    private final Short delcorrRowStatus = 1;

    @Autowired
    private BankPublishSendMsgService bankPublishSendMsgService;


    /**
     * 业务关联回写对账单信息
     *
     * @param corrData
     * @param autoTask
     * @throws Exception
     */
    @Override
    public void reWriteBankReconciliationData(CorrDataEntity corrData, boolean autoTask) throws Exception {
        // 回写银行对账单的相关状态
        BankReconciliation bankReconciliation = reWriteBankReconciliationStatus(corrData);
        // 自动关联自动确认相关逻辑
        autoassociateConfirm(bankReconciliation, corrData, autoTask);
    }

    /**
     * 修改银行对账单状态
     *
     * @param corrData
     * @return
     * @throws Exception
     */
    @Override
    public BankReconciliation reWriteBankReconciliationStatus(CorrDataEntity corrData) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(corrData.getBankReconciliationId()));
        querySchema.addCondition(group);
        List<BankReconciliation> list = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isNotEmpty(list)) {
            BankReconciliation bankReconciliation = list.get(0);
            // 设置银行对账单设置为已关联
            //改到CommonSaveUtils中统一操作
            /*bankReconciliation.setAssociationstatus(AssociationStatus.Associated.getValue());*/
            // 设置银行对账单是否为自动关联
            bankReconciliation.setAutoassociation(corrData.getAuto());
            // CZFW-368598，自动生单自动提交的，为自动生单
            if (corrData.getAuto() && corrData.getExtendFields() != null && corrData.getExtendFields().containsKey("isAutoSubimit")) {
                bankReconciliation.setRelationstatus(Relationstatus.Confirmed.getValue());
            }
            //智能对账：生单关联时，添加勾兑码
            //财资统一对账码是银企联解析过来的，不覆盖 && 冲挂账不覆盖
            if (!bankReconciliation.getIsparsesmartcheckno() && !StringUtils.isEmpty(corrData.getSmartcheckno())
                    && !(bankReconciliation.getEntrytype() != null && bankReconciliation.getEntrytype() == EntryType.CrushHang_Entry.getValue())) {
                bankReconciliation.setSmartcheckno(corrData.getSmartcheckno());
            }
            bankReconciliation.setEntrytype(bankReconciliation.getVirtualEntryType());
            Short entrytype = bankReconciliation.getEntrytype();
            //该方法里对账单一定为已关联
            //如果为已关联 且 待认领金额为0 且 入账类型为空（业务关联时）或一般入账或者冲挂账，则设置流水完结状态为已完结
            //未发布的直接修改
            if (!bankReconciliation.getIspublish()) {
                if (bankReconciliation.getAmounttobeclaimed().compareTo(BigDecimal.ZERO) == 0 &&
                        (entrytype == null || entrytype == EntryType.CrushHang_Entry.getValue() || entrytype == EntryType.Normal_Entry.getValue())) {
                    /*bankReconciliation.setSerialdealendstate(SerialdealendState.END.getValue());*/
                }
            }
            //判断是否存在其他未完结的认领单。存在则对账单完结状态不修改
            if (bankReconciliation.getIspublish()) {
                QuerySchema billclaimSchema = QuerySchema.create().addSelect("id");
                QueryConditionGroup billclaimGroup = QueryConditionGroup.and(
                        QueryCondition.name("id").not_eq(corrData.getBillClaimItemId()),
                        QueryCondition.name("items.bankbill").eq(bankReconciliation.getId()),
                        QueryCondition.name("associationstatus").not_eq(AssociationStatus.Associated.getValue())
                );
                billclaimSchema.addCondition(billclaimGroup);
                List<BillClaim> billClaimList = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, billclaimSchema, null);
                if (CollectionUtils.isEmpty(billClaimList)) {
                    if (bankReconciliation.getAmounttobeclaimed().compareTo(BigDecimal.ZERO) == 0 &&
                            (entrytype == null || entrytype == EntryType.CrushHang_Entry.getValue() || entrytype == EntryType.Normal_Entry.getValue())) {
                        /*bankReconciliation.setSerialdealendstate(SerialdealendState.END.getValue());*/
                    }
                }
                //发布待办消息：待办消息推送为已办理
                try {
                    bankPublishSendMsgService.handleConfirmMsg(bankReconciliation);
                } catch (Exception e) {
                    log.error("BankPublishSendMsgService handleConfirmMsg error:{}", e.getMessage());
                }
            }
            if (corrData.isGenerate()) {
                bankReconciliation.setSerialdealtype(ClaimCompleteType.RecePayGen.getValue());
            } else {
                bankReconciliation.setSerialdealtype(ClaimCompleteType.RecePayAssociated.getValue());
            }
            // 还原完成
            CommonSaveUtils.updateBankReconciliation(bankReconciliation);
            return bankReconciliation;
        }
        return null;
    }

    /**
     * 自动关联自动确认的相关逻辑
     *
     * @param bankReconciliation
     * @param corrData
     * @param autoTask
     * @throws Exception
     */
    private void autoassociateConfirm(BankReconciliation bankReconciliation, CorrDataEntity corrData, boolean autoTask) throws Exception {
        //自动关联确认操作
        //查询自动化参数配置表
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("accentity").eq(bankReconciliation.getAccentity()));
        querySchema1.addCondition(group1);
        List<AutoConfig> config = MetaDaoHelper.queryObject(AutoConfig.ENTITY_NAME, querySchema1, null);
        if (autoTask && config.size() > 0 && CollectionUtils.isNotEmpty(config)) {
            //判断“自动关联后确认”字段值
            if (config.get(0).getAutoassociateconfirm() != null && config.get(0).getAutoassociateconfirm()) {
                //查询对账单关联表数据
                QuerySchema querySchema2 = QuerySchema.create().addSelect("*");
                QueryConditionGroup group2 = QueryConditionGroup.and(
                        QueryCondition.name("bankreconciliation").eq(corrData.getBankReconciliationId()),
                        QueryCondition.name("billcode").eq(corrData.getCode()));
                querySchema2.addCondition(group2);
                List<BankReconciliationbusrelation_b> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema2, null);
                if (bankReconciliations.size() > 0 && CollectionUtils.isNotEmpty(bankReconciliations)) {
                    //获取自动关联确认接口参数
                    List corrIds = bankReconciliations.stream().map(BankReconciliationbusrelation_b::getId).collect(Collectors.toList());
                    List dcFlags = new ArrayList();
                    corrIds.stream().forEach(e -> dcFlags.add(corrData.getDcFlag()));
                    CorrOperationService corrOperationService = CtmAppContext.getBean(CorrOperationService.class);
                    //调用自动关联确认接口
                    corrOperationService.confirmCorrOprationUseException(corrIds, dcFlags);
                }
            }
        }

    }


    /**
     * 回写付款单数据
     *
     * @param corrData
     */
    @Override
    public void reWritePayMentData(CorrDataEntity corrData) throws Exception {
        Short associationStatus = 1;
        try {
            List<FundPayment_b> fundPaymentList;
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(corrData.getBusid().toString()));
            querySchema.addCondition(group);
            fundPaymentList = MetaDaoHelper.queryObject(FundPayment_b.ENTITY_NAME, querySchema, null);
            if (fundPaymentList != null && fundPaymentList.size() > 0) {
                FundPayment_b fundPayment = fundPaymentList.get(0);
                //更新主表pubts
                List<FundPayment> fundPayments;
                QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("id").eq(fundPayment.getMainid()));
                querySchema1.addCondition(group1);
                fundPayments = MetaDaoHelper.queryObject(FundPayment.ENTITY_NAME, querySchema1, null);
                FundPayment ft = fundPayments.get(0);
                Date data = new Date();
                ft.setModifyTime(data);
                ft.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(FundPayment.ENTITY_NAME, ft);

                //回写子表数据
                fundPayment.setAssociationStatus(associationStatus);
                if (corrData.getBillClaimItemId() != null) {
                    fundPayment.setBillClaimId(corrData.getBillClaimItemId().toString());
                    //1130认领V3。关联操作将结算状态由结算成功，更改为 已结算补单
                    fundPayment.setFundSettlestatus(FundSettleStatus.SettlementSupplement);
                    fundPayment.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(fundPayment.getFundSettlestatus()));
                } else {
                    fundPayment.setBankReconciliationId(corrData.getBankReconciliationId().toString());
                    //退票设置结算状态为退票
                    BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, corrData.getBankReconciliationId());
                    if (bankReconciliation.getRefundstatus() != null && bankReconciliation.getRefundstatus().equals(RefundStatus.Refunded.getValue())) {
                        fundPayment.setFundSettlestatus(FundSettleStatus.Refund);
                        fundPayment.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(fundPayment.getFundSettlestatus()));
                        fundPayment.setRefundSum(fundPayment.getOriSum());//退票金额
                    } else {
                        //1130认领V3。关联操作将结算状态由结算成功，更改为 已结算补单
                        fundPayment.setFundSettlestatus(FundSettleStatus.SettlementSupplement);
                        fundPayment.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(fundPayment.getFundSettlestatus()));
                    }
                }
                fundPayment.setEntityStatus(EntityStatus.Update);
                //智能对账：资金付款单明细添加智能勾兑号
                fundPayment.setSmartcheckno(corrData.getSmartcheckno());
                MetaDaoHelper.update(FundPayment_b.ENTITY_NAME, fundPayment);
            }
        } catch (Exception e) {
            log.error("回写资金付款单错误：：" + e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101222"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000C1", "回写资金付款单错误：") /* "回写资金付款单错误：" */ + e.getMessage());
        }
    }

    /**
     * 回写收款单数据
     *
     * @param corrData
     */
    @Override
    public void reWriteFundCollectionData(CorrDataEntity corrData) throws Exception {
        Short associationStatus = 1;
        try {
            List<FundCollection_b> fundCollectionList;
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(corrData.getBusid().toString()));
            querySchema.addCondition(group);
            fundCollectionList = MetaDaoHelper.queryObject(FundCollection_b.ENTITY_NAME, querySchema, null);
            if (fundCollectionList != null && fundCollectionList.size() > 0) {
                FundCollection_b fundCollection = fundCollectionList.get(0);

                //更新主表pubts
                List<FundCollection> fundCollections;
                QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("id").eq(fundCollection.getMainid()));
                querySchema1.addCondition(group1);
                fundCollections = MetaDaoHelper.queryObject(FundCollection.ENTITY_NAME, querySchema1, null);
                FundCollection fc = fundCollections.get(0);
                Date data = new Date();
                fc.setModifyTime(data);
                fc.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(FundCollection.ENTITY_NAME, fc);

                //回写子表数据
                fundCollection.setAssociationStatus(associationStatus);
                if (corrData.getBillClaimItemId() != null) {
                    fundCollection.setBillClaimId(corrData.getBillClaimItemId().toString());
                    //1130认领V3。关联操作将结算状态由结算成功，更改为 已结算补单
                    fundCollection.setFundSettlestatus(FundSettleStatus.SettlementSupplement);
                    fundCollection.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(fundCollection.getFundSettlestatus()));
                } else {
                    //退票设置结算状态为退票
                    //BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, corrData.getBankReconciliationId());
                    //1130认领V3。关联操作将结算状态由结算成功，更改为 已结算补单
                    fundCollection.setFundSettlestatus(FundSettleStatus.SettlementSupplement);
                    fundCollection.put("stwbSettleStatus", SettleStatusConverter.convertToSettleApplyDetailStateEnum(fundCollection.getFundSettlestatus()));
                    fundCollection.setBankReconciliationId(corrData.getBankReconciliationId().toString());
                }
                fundCollection.setEntityStatus(EntityStatus.Update);
                //智能对账：资金付款单明细添加智能勾兑号
                fundCollection.setSmartcheckno(corrData.getSmartcheckno());
                MetaDaoHelper.update(FundCollection_b.ENTITY_NAME, fundCollection);
            }
        } catch (Exception e) {
            log.error("回写资金收款单错误：：" + e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101223"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000C3", "回写资金收款单错误：") /* "回写资金收款单错误：" */ + e.getMessage());
        }
    }

    /**
     * 回写转账单数据
     *
     * @param corrData
     */
    @Override
    public void reWriteTransferAccountData(CorrDataEntity corrData) throws Exception {
        try {
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(corrData.getMainid()));
            querySchema.addCondition(group);
            List<TransferAccount> transferAccountList = MetaDaoHelper.queryObject(TransferAccount.ENTITY_NAME, querySchema, null);
            if (transferAccountList != null && transferAccountList.size() > 0) {
                TransferAccount transferAccount = transferAccountList.get(0);
                //更改 pubts
                Date date = new Date();
                transferAccount.setModifyTime(date);
                //判断借贷方向
                if ((short) 1 == corrData.getDcFlag()) { //借：付
                    //更改关联状态 和 对账单id 要判断借贷方向
                    //先判断是否已关联 如果已经关联，则关联失败
                    if (transferAccount.getAssociationStatusPay()) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101224"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000C7", "此转账单付款已关联，请关联其他单据！") /* "此转账单付款已关联，请关联其他单据！" */);
                    } else {
                        transferAccount.setAssociationStatusPay(true);
                    }
                    //更改认领单id 要判断借贷方向
                    if (corrData.getBillClaimItemId() != null) {
                        transferAccount.setPaybillclaim(corrData.getBillClaimItemId());
                    } else {
                        transferAccount.setPaybankbill(corrData.getBankReconciliationId());
                    }
                    transferAccount.setPaysmartcheckno(corrData.getSmartcheckno());
                } else { //贷：收
                    //更改关联状态 和 对账单id 要判断借贷方向
                    if (transferAccount.getAssociationStatusCollect()) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101225"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000C4", "此转账单收款已关联，请关联其他单据！") /* "此转账单收款已关联，请关联其他单据！" */);
                    } else {
                        transferAccount.setAssociationStatusCollect(true);
                    }
                    //更改认领单id 要判断借贷方向
                    if (corrData.getBillClaimItemId() != null) {
                        transferAccount.setCollectbillclaim(corrData.getBillClaimItemId());
                    } else {
                        transferAccount.setCollectbankbill(corrData.getBankReconciliationId());
                    }
                    transferAccount.setSmartcheckno(corrData.getSmartcheckno());
                }
                transferAccount.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccount);
            }
        } catch (Exception e) {
            log.error("回写转账单错误：" + e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101226"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000C6", "回写转账单错误：") /* "回写转账单错误：" */ + e.getMessage());

        }
    }

    /**
     * 回写认领单数据
     *
     * @param id
     * @param associationStatus
     * @throws Exception
     */
    @Override
    public void reWriteBillClaimData(Long id, Short associationStatus, String smartcheckno, Short claimcompletetype) throws Exception {

        // 判断是否是应收收款单回写  适配应收收款单回传认领单id的情况--liuwtr
        Boolean yinshouFlag = Boolean.FALSE;
        if (smartcheckno != null && smartcheckno.contains(",")) {
            yinshouFlag = Boolean.TRUE;
            String[] strs = smartcheckno.split(",");
            smartcheckno = strs[0];
        }

        try {
            //Map reqMap = new HashMap();
            List<BillClaim> list;
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(id.toString()));

            querySchema.addCondition(group);
            list = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, querySchema, null);
            if (list != null && list.size() > 0) {
                // 应收收款单回写，认领单参照单据id不为空
                if (yinshouFlag && StringUtils.isNotBlank(list.get(0).getRefbill())) {
                    // 回写为关联状态
                    if (associationStatus == AssociationStatus.Associated.getValue()) {
                        // 参照关联状态为已关联，说明回写业务关联状态
                        if (list.get(0).getRefassociationstatus() != null && list.get(0).getRefassociationstatus() == AssociationStatus.Associated.getValue()) {
                            list.get(0).setAssociationstatus(associationStatus);
                        } else {
                            list.get(0).setRefassociationstatus(associationStatus);
                        }
                    } else {
                        // 回写为未关联状态
                        // 业务关联状态为已关联  回写为未关联
                        if (list.get(0).getAssociationstatus() != null && list.get(0).getAssociationstatus() == AssociationStatus.Associated.getValue()) {
                            list.get(0).setAssociationstatus(associationStatus);
                        } else {
                            list.get(0).setRefassociationstatus(associationStatus);
                        }
                    }
                } else {
                    list.get(0).setAssociationstatus(associationStatus);
                }
                //智能对账：认领单智能勾兑码赋值
                list.get(0).setSmartcheckno(smartcheckno);
                //认领完结方式
                if (associationStatus == AssociationStatus.NoAssociated.getValue()) {
                    list.get(0).setClaimcompletetype(null);
                    list.get(0).setAssociatedoperator(null);
                    list.get(0).setAssociateddate(null);
                } else {
                    list.get(0).setClaimcompletetype(claimcompletetype);
                    list.get(0).setAssociatedoperator(InvocationInfoProxy.getUserid());
                    list.get(0).setAssociateddate(new Date());
                }
                list.get(0).setEntityStatus(EntityStatus.Update);
                CommonSaveUtils.updateBillClaim(list.get(0));
            }

            List<BillClaim> list2;
            //回写参照关联状态
            QuerySchema querySchema2 = QuerySchema.create().addSelect("*");
            QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("refbill").eq(id.toString()));
            querySchema2.addCondition(group2);
            list2 = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, querySchema2, null);
            if (list2 != null && list2.size() > 0) {
                list2.get(0).setRefassociationstatus(associationStatus);
                list2.get(0).setEntityStatus(EntityStatus.Update);
                CommonSaveUtils.updateBillClaim(list2.get(0));
            }

        } catch (Exception e) {
            log.error("回写认领单数据错误：：" + e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101227"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000C5", "回写认领单数据错误：") /* "回写认领单数据错误：" */ + e.getMessage());
        }

    }

    /**
     * 删除关联关系表
     *
     * @param json
     * @throws Exception
     */
    @Override
    public void resDelData(CtmJSONObject json) throws Exception {
        Long busid = json.getLong("busid"); //银行对账单id
        Long stwbbusid = json.getLong("stwbbusid");// 业务单据id
        String claimid = json.getString("claimid");//认领到id
        List<BankReconciliationbusrelation_b> bs = new ArrayList<BankReconciliationbusrelation_b>();
        String smartCheckNo = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();
        try {
            if (claimid != null) {
                //认领单id不为空，关联认领单
                //回写认领单
                List<BillClaim> billClaims;
                QuerySchema bquery = QuerySchema.create().addSelect("*");
                QueryConditionGroup querygroup = QueryConditionGroup.and(QueryCondition.name("id").eq(claimid));
                bquery.addCondition(querygroup);
                billClaims = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, bquery, null);
                if (CollectionUtils.isNotEmpty(billClaims)) {
                    //修改认领单的状态
                    billClaims.get(0).setAssociationstatus(delcorrStatus);
                    billClaims.get(0).setClaimcompletetype(null);
                    billClaims.get(0).setSettlestatus(null);
                    billClaims.get(0).setEntityStatus(EntityStatus.Update);
                    //智能对账：关联删除智能勾兑码删除
                    billClaims.get(0).setSmartcheckno(smartCheckNo);
                    CommonSaveUtils.updateBillClaim(billClaims.get(0));
                }

                // 参照关联状态回写
                QuerySchema bquery2 = QuerySchema.create().addSelect("*");
                QueryConditionGroup querygroup2 = QueryConditionGroup.and(QueryCondition.name("refbill").eq(claimid));
                bquery2.addCondition(querygroup2);
                billClaims = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, bquery2, null);
                if (billClaims != null && billClaims.size() > 0) {
                    billClaims.get(0).setRefassociationstatus(delcorrStatus);
                    billClaims.get(0).setSettlestatus(null);
                    billClaims.get(0).setEntityStatus(EntityStatus.Update);
                    CommonSaveUtils.updateBillClaim(billClaims.get(0));
                }

                //查找子表数据取出对账单id
                List<BillClaimItem> list;
                QuerySchema querySchema = QuerySchema.create().addSelect("*");
                QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(claimid));
                querySchema.addCondition(group);
                list = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema, null);
                if (CollectionUtils.isNotEmpty(list)) {
                    for (BillClaimItem bill : list) {
                        //获取银行对账单的id
                        busid = bill.getBankbill();
                        List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = new ArrayList<>();
                        if (!Objects.isNull(stwbbusid)) {
                            QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                            QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("billid").eq(stwbbusid));
                            querySchema1.addCondition(group1);
                            //查询关联关系表数据
                            bankReconciliationbusrelation_bs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema1, null);
                            if (CollectionUtils.isNotEmpty(bankReconciliationbusrelation_bs)) {
                                for (BankReconciliationbusrelation_b b : bankReconciliationbusrelation_bs) {
                                    bs.add(b);
                                    YtsContext.setYtsContext("corrData", bs);
                                    MetaDaoHelper.delete(BankReconciliationbusrelation_b.ENTITY_NAME, b);
                                }
                            } else {
                                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C0004E", "根据流水id未查询到对应的关联关系！") /* "根据流水id未查询到对应的关联关系！" */);
                            }
                        }
                        QuerySchema querySchema2 = QuerySchema.create().addSelect("*");
                        QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("id").eq(busid));
                        querySchema2.addCondition(group2);
                        List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema2, null);
                        boolean needUpdate = false;
                        BankReconciliation newBankReconciliation = new BankReconciliation(busid);
                        //认领单关联关系删除对账单一定为未完结
                        if (bankReconciliations != null) {
                            newBankReconciliation.setSerialdealendstate(SerialdealendState.UNEND.getValue());
                            newBankReconciliation.setSerialdealtype(null);
                            newBankReconciliation.setGenertbilltype(null);
                            needUpdate = true;
                        }
                        if (bankReconciliations != null && bankReconciliations.size() > 0 && bankReconciliationbusrelation_bs.size() < 2) {
                            /*bankReconciliations.get(0).setAssociationstatus(delcorrStatus);*/
                            //财资统一对账码：非解析过来的和非冲挂账的清空
                            if (!bankReconciliations.get(0).getIsparsesmartcheckno()
                                    && !(bankReconciliations.get(0).getEntrytype() != null && bankReconciliations.get(0).getEntrytype() == EntryType.CrushHang_Entry.getValue())) {
                                newBankReconciliation.setSmartcheckno(smartCheckNo);
                            }
                        }
                        if (needUpdate) {
                            CommonSaveUtils.updateBankReconciliation(newBankReconciliation);
                        }
                    }
                    if (bs != null && bs.size() > 0) {
                        List<String> idList = bs.stream()
                                .map(b -> b.getId().toString())
                                .collect(Collectors.toList());
                        QuerySchema query = QuerySchema.create().addSelect("id");
                        QueryConditionGroup groupco = QueryConditionGroup.and(QueryCondition.name("id").in(idList));
                        query.addCondition(groupco);
                        List<BankReconciliationbusrelation_b> bankReconciliationRelationList = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, query, null);
                        if (CollectionUtils.isNotEmpty(bankReconciliationRelationList)) {
                            CommonSaveUtils.batchDeleteBankReconciliationbusrelation_b(bankReconciliationRelationList);
                        }
                    }
                } else {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C0004F", "根据认领单id未查询到对应的认领单信息！") /* "根据认领单id未查询到对应的认领单信息！" */);
                }
            } else {// 关联银行对账单
                if (!Objects.isNull(stwbbusid)) {
                    QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                    QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("billid").eq(stwbbusid));
                    querySchema1.addCondition(group1);
                    List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema1, null);
                    if (CollectionUtils.isNotEmpty(bankReconciliationbusrelation_bs)) {
                        for (BankReconciliationbusrelation_b b : bankReconciliationbusrelation_bs) {
                            bs.add(b);
                            YtsContext.setYtsContext("corrData", bs);
                            MetaDaoHelper.delete(BankReconciliationbusrelation_b.ENTITY_NAME, b);
                        }
                    } else {
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C0004E", "根据流水id未查询到对应的关联关系！") /* "根据流水id未查询到对应的关联关系！" */);
                    }
                }
                QuerySchema querySchema2 = QuerySchema.create().addSelect("*");
                QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("id").eq(busid));
                querySchema2.addCondition(group2);
                List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema2, null);
                if (CollectionUtils.isNotEmpty(bankReconciliations)) {
                    BankReconciliation bankReconciliation = bankReconciliations.get(0);
                    if (bankReconciliation.getEntrytype() != null) {
                        if (bankReconciliation.getEntrytype() == EntryType.Normal_Entry.getValue()) {//正常入账
                            bankReconciliation.setEntrytype(null);
                            bankReconciliation.setAssociationstatus(AssociationStatus.NoAssociated.getValue());
                        } else if (bankReconciliation.getEntrytype() == EntryType.Hang_Entry.getValue()) {//挂账
                            bankReconciliation.setEntrytype(null);
                            bankReconciliation.setIsadvanceaccounts(false);
                            bankReconciliation.setAssociationstatus(AssociationStatus.NoAssociated.getValue());
                        } else if (bankReconciliation.getEntrytype() == EntryType.CrushHang_Entry.getValue()) {//冲挂账
                            if (json.get("entrytype") != null && json.getShort("entrytype") == EntryType.Hang_Entry.getValue()) {
                                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C00051", "当前流水已发送后续冲挂账业务，无法删除，请重新检查数据！") /* "当前流水已发送后续冲挂账业务，无法删除，请重新检查数据！" */);
                            }
                            bankReconciliation.setEntrytype(EntryType.Hang_Entry.getValue());
                            bankReconciliation.setVirtualEntryType(null);
                        }
                    } else {
                        // 为空代表正常入账
                        bankReconciliation.setEntrytype(null);
                        bankReconciliation.setAssociationstatus(AssociationStatus.NoAssociated.getValue());
                        bankReconciliation.setSerialdealtype(null);
                    }
//                    newBankReconciliation.setEntityStatus(EntityStatus.Update);
                    bankReconciliation.setSerialdealendstate(SerialdealendState.UNEND.getValue());
                    CommonSaveUtils.updateBankReconciliation(bankReconciliation);
                } else {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A29C04C00050", "根据流水id未查询到对应的流水信息！") /* "根据流水id未查询到对应的流水信息！" */);
                }
            }
        } catch (Exception e) {
            log.error("删除回写报错：：" + e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101228"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418028B", "执行程序出错") /* "执行程序出错" */);
        }
    }


    /**
     * 外币兑换单回写
     *
     * @param corrEntity
     * @throws Exception
     */
    @Override
    public void reWriteCurrencyexchangeData(CorrDataEntity corrEntity) throws Exception {
        //String total = "*";
        //外币兑换单id
        Long cid = corrEntity.getBusid();
        //银行对账单id
        Long bid = corrEntity.getBankReconciliationId();

        //获取借贷方向，默认被合并认领的认领单的借贷方向一定都一致
        short flag = corrEntity.getDcFlag();

        //外币兑换单没有子表
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("id").eq(cid));
        querySchema1.addCondition(group1);

        List<CurrencyExchange> currencyExchangeList = MetaDaoHelper.queryObject(CurrencyExchange.ENTITY_NAME, querySchema1, null);

        if (currencyExchangeList != null && currencyExchangeList.size() > 0) {
            CurrencyExchange currencyExchange = currencyExchangeList.get(0);

            if (corrEntity.getBillClaimItemId() != null) {
                //根据银行对账单借贷方向判断记录银行对账单ID，借时记录到付款单ID，贷时记录到收款单ID
                if (flag == 1) {
                    //1为借，2为贷，借为付款，贷为收款
                    currencyExchange.setPaybankbill(corrEntity.getBankReconciliationId());
                    //付款单是否关联
                    currencyExchange.setAssociationStatusPay((short) 1);
                    //付款方认领单ID
                    currencyExchange.setPaybillclaim(corrEntity.getBillClaimItemId());
                    // 更新财资统一码
                    currencyExchange.setSellsmartcheckno(corrEntity.getSmartcheckno());
                } else {
                    currencyExchange.setCollectbankbill(corrEntity.getBankReconciliationId());
                    //收款单是否关联
                    currencyExchange.setAssociationStatusCollect((short) 1);
                    //收款方认领单ID
                    currencyExchange.setCollectbillclaim(corrEntity.getBillClaimItemId());
                    // 更新财资统一码
                    currencyExchange.setBuysmartcheckno(corrEntity.getSmartcheckno());
                }
            } else {
                //1为借，2为贷,借为付款，贷为收款
                if (flag == 1) {
                    //根据银行对账单借贷方向判断记录银行对账单ID，借时记录到付款单ID，贷时记录到收款单ID
                    currencyExchange.setPaybankbill(bid);
                    //付款单是否关联
                    currencyExchange.setAssociationStatusPay((short) 1);
                    // 更新财资统一码
                    currencyExchange.setSellsmartcheckno(corrEntity.getSmartcheckno());
                } else {
                    //收款单id
                    currencyExchange.setCollectbankbill(bid);
                    //收款单是否关联
                    currencyExchange.setAssociationStatusCollect((short) 1);
                    // 更新财资统一码
                    currencyExchange.setBuysmartcheckno(corrEntity.getSmartcheckno());
                }
            }

            //更新外币兑换单更新状态
            currencyExchange.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, currencyExchange);
        }
    }

    /**
     * 回写付款单关联状态
     *
     * @param corrData
     * @throws Exception
     */
    @Override
    public void reWritePayBillData(CorrDataEntity corrData) throws Exception {
        PayBill payBill = MetaDaoHelper.findById(PayBill.ENTITY_NAME, corrData.getMainid());
        if (payBill == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101229"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000C2", "单据不存在 id:") /* "单据不存在 id:" */ + corrData.getMainid());
        }
        if (AssociationStatus.Associated.getValue() == payBill.getAssociationStatus()) {
            //合并认领会多次处理，已关联的直接返回
            return;
//            throw new CtmException("此单据已关联，请关联其他单据！");
        }
        if (corrData.getBillClaimItemId() != null) {
            payBill.setBillClaimId(corrData.getBillClaimItemId() + "");
        } else {
            payBill.setBankReconciliationId(corrData.getBankReconciliationId() + "");
        }
        payBill.setAssociationStatus(AssociationStatus.Associated.getValue());
        payBill.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(PayBill.ENTITY_NAME, payBill);

    }

    /**
     * 回写收款单关联状态
     *
     * @param corrData
     * @throws Exception
     */
    @Override
    public void reWriteReceiveBillData(CorrDataEntity corrData) throws Exception {
        ReceiveBill receiveBill = MetaDaoHelper.findById(ReceiveBill.ENTITY_NAME, corrData.getMainid());
        if (receiveBill == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101229"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000C2", "单据不存在 id:") /* "单据不存在 id:" */ + corrData.getMainid());
        }
        if (AssociationStatus.Associated.getValue() == receiveBill.getAssociationStatus()) {
            //合并认领会多次处理，已关联的直接返回
            return;
//            throw new CtmException("此单据已关联，请关联其他单据！");
        }
        if (corrData.getBillClaimItemId() != null) {
            receiveBill.setBillClaimId(corrData.getBillClaimItemId() + "");
        } else {
            receiveBill.setBankReconciliationId(corrData.getBankReconciliationId() + "");
        }
        receiveBill.setAssociationStatus(AssociationStatus.Associated.getValue());
        receiveBill.setEntityStatus(EntityStatus.Update);
        MetaDaoHelper.update(ReceiveBill.ENTITY_NAME, receiveBill);
    }

    /**
     * 回写银行对账单关联关系
     * 同名账户划转: 银行转账单,缴存现金单（收款流水）,提取现金单（付款流水）,第三方转账
     *
     * @param corrDataList
     */
    @Override
    public void reWriteBankReconciliationRelationData(List<CorrDataEntity> corrDataList) throws Exception {
        if (org.springframework.util.CollectionUtils.isEmpty(corrDataList)) {
            return;
        }
        CorrOperationService corrOperationService = CtmAppContext.getBean(CorrOperationService.class);
        for (CorrDataEntity corrEntity : corrDataList) {
            //回写银行对账单主表
            this.reWriteBankReconData(corrEntity);
            //关联数据新增
            corrOperationService.reWriteTransferAccCorrelationOperation(corrEntity);
            //回写转账单数据
            this.reWriteTransferAccountDataInfo(corrEntity);

        }


    }

    /**
     * 回写银行对账单数据
     * 同名账户划转: 银行转账单,缴存现金单（收款流水）,提取现金单（付款流水）,第三方转账
     *
     * @param corrData
     */
    @Override
    public void reWriteBankReconData(CorrDataEntity corrData) throws Exception {
        BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, corrData.getBankReconciliationId());
        /*bankReconciliation.setAssociationstatus(AssociationStatus.Associated.getValue());*/
        bankReconciliation.setAutoassociation(corrData.getAuto());
        //智能对账：生单关联时，添加勾兑码
        bankReconciliation.setSmartcheckno(corrData.getSmartcheckno());
        // 提前入账  银行对账单回写;202409 转账单冲挂账逻辑修改
        if (bankReconciliation.getIsadvanceaccounts()) {
            bankReconciliation.setAssociationcount(AssociationCount.Second.getValue());
            //提前入账，银行流水入账类型为冲挂账
            bankReconciliation.setEntrytype(EntryType.CrushHang_Entry.getValue());
        }
        bankReconciliation.setEntityStatus(EntityStatus.Update);
        Short entrytype = bankReconciliation.getEntrytype();
        //该方法里对账单一定为已关联
        //如果为已关联 且 待认领金额为0 且 入账类型为空（业务关联时）或一般入账或者冲挂账，则设置流水完结状态为已完结
        //未发布的直接修改
        if (!bankReconciliation.getIspublish()) {
            if (bankReconciliation.getAmounttobeclaimed().compareTo(BigDecimal.ZERO) == 0 &&
                    (entrytype == null || entrytype == EntryType.CrushHang_Entry.getValue() || entrytype == EntryType.Normal_Entry.getValue())) {
                /*bankReconciliation.setSerialdealendstate(SerialdealendState.END.getValue());*/
            }
        }
        //判断是否存在其他未完结的认领单。存在则对账单完结状态不修改
        if (bankReconciliation.getIspublish()) {
            QuerySchema billclaimSchema = QuerySchema.create().addSelect("id");
            QueryConditionGroup billclaimGroup = QueryConditionGroup.and(
                    QueryCondition.name("id").not_eq(corrData.getBillClaimItemId()),
                    QueryCondition.name("items.bankbill").eq(bankReconciliation.getId()),
                    QueryCondition.name("associationstatus").not_eq(AssociationStatus.Associated.getValue())
            );
            billclaimSchema.addCondition(billclaimGroup);
            List<BillClaim> billClaimList = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, billclaimSchema, null);
            if (CollectionUtils.isEmpty(billClaimList)) {
                if (bankReconciliation.getAmounttobeclaimed().compareTo(BigDecimal.ZERO) == 0 &&
                        (entrytype == null || entrytype == EntryType.CrushHang_Entry.getValue() || entrytype == EntryType.Normal_Entry.getValue())) {
                    /*bankReconciliation.setSerialdealendstate(SerialdealendState.END.getValue());*/
                }
            }
            //发布待办消息：对方类型为客户或者供应商时,待办消息推送已办理
            try {
                bankPublishSendMsgService.handleConfirmMsg(bankReconciliation);
            } catch (Exception e) {
                log.error("BankPublishSendMsgService handleConfirmMsg error:{}", e.getMessage());
            }
        }
        if (corrData.isGenerate()) {
            bankReconciliation.setSerialdealtype(ClaimCompleteType.RecePayGen.getValue());
        } else {
            bankReconciliation.setSerialdealtype(ClaimCompleteType.RecePayAssociated.getValue());
        }
        CommonSaveUtils.updateBankReconciliation(bankReconciliation);
        //关联认领单
        if (null != corrData.getBillClaimItemId()) {
            //回写认领单
            BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, corrData.getBillClaimItemId(), null);
            billClaim.setAssociationstatus(AssociationStatus.Associated.getValue());
            billClaim.setEntityStatus(EntityStatus.Update);
            billClaim.setSmartcheckno(corrData.getSmartcheckno());
            CommonSaveUtils.updateBillClaim(billClaim);
        }
    }

    /**
     * 回写转账单数据 勾兑号
     * 同名账户划转: 银行转账单,缴存现金单（收款流水）,提取现金单（付款流水）,第三方转账
     *
     * @param corrData
     */
    @Override
    public void reWriteTransferAccountDataInfo(CorrDataEntity corrData) throws Exception {
        try {
            TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, corrData.getMainid());
            if (null == transferAccount) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101230"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080109", "未查询到转账单信息，请确认！id:") /* "未查询到转账单信息，请确认！id:" */ + corrData.getMainid());
            }
            transferAccount.setModifyTime(new Date());
            //判断借贷方向
            if (DirectionJD.Debit.getValue() == corrData.getDcFlag()) {
                //判断是否未关联 支出
                if (!transferAccount.getAssociationStatusPay()) {
                    transferAccount.setAssociationStatusPay(true);
                }
                //设置【付款】勾对码
                transferAccount.setPaysmartcheckno(corrData.getSmartcheckno());
            } else {
                //判断是否未关联
                if (!transferAccount.getAssociationStatusCollect()) {
                    transferAccount.setAssociationStatusCollect(true);
                }
                //设置【收款】勾对码
                transferAccount.setSmartcheckno(corrData.getSmartcheckno());
            }
            transferAccount.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(TransferAccount.ENTITY_NAME, transferAccount);
        } catch (Exception e) {
            log.error("回写转账单错误：" + e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101226"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000C6", "回写转账单错误：") /* "回写转账单错误：" */ + e.getMessage());

        }
    }
}
