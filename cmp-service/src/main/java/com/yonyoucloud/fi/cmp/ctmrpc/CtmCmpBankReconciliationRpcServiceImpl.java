package com.yonyoucloud.fi.cmp.ctmrpc;

import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.api.IYmsJdbcApi;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.uap.billcode.BillCodeComponentParam;
import com.yonyou.uap.billcode.BillCodeObj;
import com.yonyou.uap.billcode.service.IBillCodeComponentService;
import com.yonyou.ucf.mdd.ext.bill.billmake.service.MakeBillRuleClientService;
import com.yonyou.ucf.mdd.ext.bill.billmake.vo.PushAndPullVO;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.bpm.service.ProcessService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.remote.RemoteDubbo;
import com.yonyou.yonbip.ctm.security.signature.CtmSignatureService;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyoucloud.ctm.stwb.reconcode.pubitf.ReconciliateCodeGenerator;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.autocorrsetting.CorrOperationService;
import com.yonyoucloud.fi.cmp.autocorrsetting.ReWriteBusCorrDataService;
import com.yonyoucloud.fi.cmp.autocorrsettings.BussDocumentType;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetail;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetailService;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.bankreceipt.service.BankReceiptService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationbusrelation_b;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankreconciliationService;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.bankreconciliation.service.autogenerateBill.BankAutoPushBillServiceImpl;
import com.yonyoucloud.fi.cmp.bankreconciliation.utils.CommonBankReconciliationProcessor;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankrecilication.CtmCmpBankReconciliationRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.*;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonResponseDataVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.BankLinkParam;
import com.yonyoucloud.fi.cmp.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yonyou.bpm.rest.RepositoryService;
import yonyou.bpm.rest.request.repository.ProcessDefinitionQueryParam;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 本类用于供账户管理调用，查询银行对账单下会计主体和银行账号对应期间的交易笔数及最大交易金额
 */
@Slf4j
@Service
public class CtmCmpBankReconciliationRpcServiceImpl implements CtmCmpBankReconciliationRpcService {

    private static final String BANKRECONCILIATIONMAPPER = "com.yonyoucloud.fi.cmp.bankreconciliation.rule.BankReconciliationMapper";

    @Autowired
    CorrOperationService corrOperationService;//写入关联关系
    @Autowired
    ReWriteBusCorrDataService reWriteBusCorrDataService;

    @Autowired
    YmsOidGenerator ymsOidGenerator;

    @Autowired
    MakeBillRuleClientService makeBillRuleClientService;
    @Autowired
    ProcessService processService;
    @Autowired
    CmCommonService cmCommonService;
    @Resource
    private IFundCommonService fundCommonService;
    @Resource
    private BankAutoPushBillServiceImpl bankAutoPushBillService;
    @Autowired
    private CtmSignatureService signatureService;

    @Autowired
    BankDealDetailService bankDealDetailService;

    @Autowired
    BankreconciliationService bankreconciliationService;
    @Resource
    @Qualifier("busiBaseDAO")
    private IYmsJdbcApi ymsJdbcApi;
    @Autowired
    BankReceiptService bankReceiptService;


    @Override
    public CommonResponseDataVo queryBankReconciliationUseInfo(CommonRequestDataVo commonQueryData) throws Exception {
        CommonResponseDataVo result = new CommonResponseDataVo();
        String accentity = commonQueryData.getAccentity();//会计主体
        String bankaccount = commonQueryData.getBankaccount();//银行账号
        String currency = commonQueryData.getCurrency(); //币种
//        Short dcflag = commonQueryData.getDcflag();//借贷方向
        String remark = commonQueryData.getRemark(); //摘要
        String remarkMatchType = commonQueryData.getMatchType();// 摘要匹配方式
        String startDate = commonQueryData.getStartDate();
        String endDate = commonQueryData.getEndDate();
        commonQueryData.setYtenantId(InvocationInfoProxy.getTenantid());
        if (StringUtils.isEmpty(accentity)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102246"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807E7", "会计主体不可为空！") /* "会计主体不可为空！" */);
        }
        if (StringUtils.isEmpty(bankaccount)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102247"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807E9", "银行账号不可为空！") /* "银行账号不可为空！" */);
        }
        if (StringUtils.isEmpty(currency)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102248"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807EA", "币种不可为空！") /* "币种不可为空！" */);
        }
//        if(dcflag == null){
//            throw new CtmException("借贷方向不可为空！");
//        }
        if (startDate == null || endDate == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102249"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807DF", "交易日期区间不可为空！") /* "交易日期区间不可为空！" */);
        }
        if(CtmDealDetailCheckMayRepeatUtils.isRepeatCheck) {
            CtmJSONObject info = SqlHelper.selectOne(BANKRECONCILIATIONMAPPER + ".queryBankReconciliationUseInfoWithRepeatCondition", commonQueryData);
            result.setTotalCount(info.getLong("count"));
            result.setMaxSum(info.getBigDecimal("maxamt"));
        }else{
            CtmJSONObject info = SqlHelper.selectOne(BANKRECONCILIATIONMAPPER + ".queryBankReconciliationUseInfo", commonQueryData);
            result.setTotalCount(info.getLong("count"));
            result.setMaxSum(info.getBigDecimal("maxamt"));
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public void autoCorr(Map paramMap) throws Exception {
        //主表id,对账单id
        Long bid = (Long) paramMap.get("bid");
        //子表id，收付款结算单明细id
        Long busid = (Long) paramMap.get("busid");
        //单据类型，0 收款 1 付款
        String billType = (String) paramMap.get("billType");
        Date pubts = new Date();
        Date pubtsm = new Date();
        if ("1".equals(billType)) {
//            List<FundPayment_b> fundPaymentList = new ArrayList<FundPayment_b>();
//            QuerySchema querySchema = QuerySchema.create().addSelect("*");
//            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(busid));
//            querySchema.addCondition(group);
            FundPayment_b fundPayment = MetaDaoHelper.findById(FundPayment_b.ENTITY_NAME, busid);
            //主表时间戳
            pubts = fundPayment.getPubts();

//            FundPayment_b fundPayment = fundPaymentList.get(0);
            String id = fundPayment.getMainid();
//            QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
//            QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("id").eq(id));
//            querySchema1.addCondition(group1);
            FundPayment fundPayment1 = MetaDaoHelper.findById(FundPayment.ENTITY_NAME, id);
            //子表时间戳
            pubtsm = fundPayment1.getPubts();
        } else if ("0".equals(billType)) {
//            List<FundCollection_b> fundCollectionList = new ArrayList<FundCollection_b>();
//            QuerySchema querySchema = QuerySchema.create().addSelect("*");
//            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(busid));
//            querySchema.addCondition(group);
            FundCollection_b fundCollection_b = MetaDaoHelper.findById(FundCollection_b.ENTITY_NAME, busid);
            //主表时间戳
            pubts = fundCollection_b.getPubts();

//            FundCollection_b fundCollection = fundCollectionList.get(0);
            String id = fundCollection_b.getMainid();
//            QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
//            QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("id").eq(id));
//            querySchema1.addCondition(group1);
            FundCollection fundCollection1 = MetaDaoHelper.findById(FundCollection.ENTITY_NAME, id);
            //子表时间戳
            pubtsm = fundCollection1.getPubts();
        }
        String key = "Maunaul_" + bid.toString();
        //借贷方向
        Short dcFlag = paramMap.containsKey("dcFlag") ? (Short) paramMap.get("dcFlag") : null;
        Map<String, String> mapres = new HashMap<>();
        Boolean isEdited = false;
        YmsLock ymsLock = null;
        try {
            if ((ymsLock= JedisLockUtils.lockBillWithOutTrace(key))==null) {
                isEdited = true;
            }
            List<CorrDataEntity> corrDataEntities;
            //处理资金收付款单
            corrDataEntities = manualAssociatedData(billType, bid, busid, pubts, pubtsm, dcFlag);
            //业务单据回写
            manualCorrBill(corrDataEntities, paramMap);
        } catch (Exception e) {
            log.error("手动关联出错：" + e, e);
            isEdited = true;
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        if (isEdited) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102250"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807E4", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
        }
    }

    private List<CorrDataEntity> manualAssociatedData(String billType, Long bid, Long busid, Date pubts, Date pubts1, Short dcFlag) throws Exception {
        FundPayment_b fundPayment = new FundPayment_b();
        FundCollection_b fundCollection = new FundCollection_b();
        TransferAccount transferAccount = new TransferAccount();
        CorrDataEntity entity = new CorrDataEntity();
        List<CorrDataEntity> resList = new ArrayList<CorrDataEntity>();
        //智能对账：生成勾兑码
//        String smartcheckno = UUID.randomUUID().toString().replace("-", "");
        //调用资金结算财资统一对账码接口生成
        String smartcheckno = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();
        if ("1".equals(billType)) { //资金付款单
            List<FundPayment_b> fundPaymentList = new ArrayList<FundPayment_b>();
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(busid));
            querySchema.addCondition(group);
            fundPaymentList = MetaDaoHelper.query(FundPayment_b.ENTITY_NAME, querySchema);
            if (fundPaymentList != null && fundPaymentList.size() > 0) {
                fundPayment.init(fundPaymentList.get(0));
                String id = fundPayment.getMainid();
                QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("id").eq(id));
                querySchema1.addCondition(group1);
                List<FundPayment> fundPaymentList1 = MetaDaoHelper.query(FundPayment.ENTITY_NAME, querySchema1);
                if (fundPaymentList1 != null && fundPaymentList1.size() > 0) {
                    FundPayment fundPayment1 = new FundPayment();
                    fundPayment1.init(fundPaymentList1.get(0));
                    //校验pubts单据是否是最新状态
                    Date fundMainpubts = fundPayment1.getPubts();
                    Date fundpubts = fundPayment.getPubts();
                    if (fundpubts.compareTo(pubts) != 0 || fundMainpubts.compareTo(pubts1) != 0) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102250"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807E4", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                    }
                    entity.setAuto(false);
                    entity.setBillType(String.valueOf(BussDocumentType.fundpayment.getValue()));
                    entity.setBusid(fundPayment.getId());
                    entity.setCode(fundPayment1.getCode());
                    entity.setBillNum(IBillNumConstant.FUND_PAYMENT);
                    entity.setDept(fundPayment.getDept());
                    entity.setMainid(Long.valueOf(id));
                    entity.setProject(fundPayment.getProject());
                    entity.setVouchdate(fundPayment1.getVouchdate());
                    entity.setOriSum(fundPayment.getOriSum());
                    entity.setAccentity(fundPayment1.getAccentity());
                }
            }
        } else if ("0".equals(billType)) { //资金收款单
            List<FundCollection_b> fundCollectionList = new ArrayList<FundCollection_b>();
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(busid));
            querySchema.addCondition(group);
            fundCollectionList = MetaDaoHelper.query(FundCollection_b.ENTITY_NAME, querySchema);
            if (fundCollectionList != null && fundCollectionList.size() > 0) {
                fundCollection.init(fundCollectionList.get(0));
                String id = fundCollection.getMainid();
                QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("id").eq(id));
                querySchema1.addCondition(group1);
                List<FundCollection> fundCollectionList1 = MetaDaoHelper.query(FundCollection.ENTITY_NAME, querySchema1);
                if (fundCollectionList1 != null && fundCollectionList1.size() > 0) {
                    FundCollection fundCollection1 = new FundCollection();
                    fundCollection1.init(fundCollectionList1.get(0));
                    //校验pubts单据是否是最新状态
                    Date fundMainpubts = fundCollection1.getPubts();
                    Date fundpubts = fundCollection.getPubts();
                    if (fundpubts.compareTo(pubts) != 0 || fundMainpubts.compareTo(pubts1) != 0) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102250"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807E4", "当前单据不是最新状态，请刷新单据重新操作。") /* "当前单据不是最新状态，请刷新单据重新操作。" */);
                    }
                    entity.setAuto(false);
                    entity.setBillType(String.valueOf(BussDocumentType.fundcollection.getValue()));
                    entity.setBusid(fundCollection.getId());
                    entity.setCode(fundCollection1.getCode());
                    entity.setBillNum(IBillNumConstant.FUND_COLLECTION);
                    entity.setDept(fundCollection.getDept());
                    entity.setMainid(Long.valueOf(id));
                    entity.setProject(fundCollection.getProject());
                    entity.setVouchdate(fundCollection1.getVouchdate());
                    entity.setAccentity(fundCollection1.getAccentity());
                    entity.setOriSum(fundCollection.getOriSum());
                }
            }
        }

        List<BankReconciliation> list;
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("id").eq(bid));
        querySchema1.addCondition(group1);
        list = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, querySchema1);
        if (list != null && list.size() > 0) {
            BankReconciliation bankReconciliation = new BankReconciliation();
            bankReconciliation.init(list.get(0));
            entity.setBankReconciliationId(bankReconciliation.getId());
            //智能对账
            entity.setSmartcheckno(smartcheckno);
            resList.add(entity);
        }

        return resList;
    }

    private void manualCorrBill(List<CorrDataEntity> corrDataEntities, Map<String, Object> paramMap) throws Exception {

        if (corrDataEntities == null || corrDataEntities.size() < 1) {
            return;
        }
        for (CorrDataEntity corrEntity : corrDataEntities) {
            corrOpration(corrEntity);
            reWriteBankReconciliationData(corrEntity);
            if ("1".equals(corrEntity.getBillType())) {
                reWritePayMentData(corrEntity);
            } else if ("0".equals(corrEntity.getBillType())) {
                reWriteFundCollectionData(corrEntity);
            }
        }
    }

    /**
     * 业务关联操作
     *
     * @param corrData
     * @throws Exception
     */
    private void corrOpration(CorrDataEntity corrData) throws Exception {

        BankReconciliationbusrelation_b bankReconciliationbusrelationB = new BankReconciliationbusrelation_b();
        boolean updateflag = false;
        /**
         * 生单关联，需判断是否是业务单据编辑保存，如是业务单据编辑保存，执行Update
         */
        if (!corrData.getAuto()) {
            QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
            QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("bankreconciliation").eq(corrData.getBankReconciliationId()));
            querySchema1.addCondition(group1);
            List<BankReconciliationbusrelation_b> bankReconciliations = MetaDaoHelper.query(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema1);
            String status = corrData.getStatus() + "";
            if (bankReconciliations != null && bankReconciliations.size() > 0 && "Update".equals(status)) {
                /**
                 * 业务单据修改触发以下代码，找出对应业务单据的关联关系，进行修改。
                 */
                for (int i = 0; i < bankReconciliations.size(); i++) {
                    BankReconciliationbusrelation_b b = new BankReconciliationbusrelation_b();
                    b.init(bankReconciliations.get(0));
                    if (b.getBillcode().equals(corrData.getCode())) {
                        bankReconciliationbusrelationB = b;
                        break;
                    }
                }
                updateflag = true;
            }
            if (updateflag) {
                YtsContext.setYtsContext("bankReconciliationbusrelationB", SerializationUtils.clone(bankReconciliationbusrelationB));
            } else {
                YtsContext.setYtsContext("bankReconciliationbusrelationBId", bankReconciliationbusrelationB.getId());
            }
        }
        //赋值关联类型
        Short isAuto;
        if (corrData.isGenerate()) {
            isAuto = 2;
        } else {
            isAuto = corrData.getAuto() ? (short) 0 : (short) 1;
        }
        //添加数据
        bankReconciliationbusrelationB = setBankData(bankReconciliationbusrelationB, corrData);
        if (updateflag) {
            bankReconciliationbusrelationB.setEntityStatus(EntityStatus.Update);
            MetaDaoHelper.update(BankReconciliationbusrelation_b.ENTITY_NAME, bankReconciliationbusrelationB);
        } else {
            bankReconciliationbusrelationB.setId(ymsOidGenerator.nextId());
            YtsContext.setYtsContext("bankReconciliationbusrelationBId", bankReconciliationbusrelationB.getId());
            bankReconciliationbusrelationB.setRelationtype(isAuto);
            bankReconciliationbusrelationB.setEntityStatus(EntityStatus.Insert);
            //CmpMetaDaoHelper.insert(BankReconciliationbusrelation_b.ENTITY_NAME, bankReconciliationbusrelationB);
            List<BankReconciliationbusrelation_b> relationList = new ArrayList<>();
            relationList.add(bankReconciliationbusrelationB);
            CommonSaveUtils.insertBankReconciliationbusrelation_b(relationList);
        }
    }


    /**
     * 业务关联回写对账单信息
     *
     * @param corrData
     * @throws Exception
     */
    private void reWriteBankReconciliationData(CorrDataEntity corrData) throws Exception {
        List<BankReconciliation> list;
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(corrData.getBankReconciliationId()));
        querySchema.addCondition(group);
        list = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, querySchema);
        BankReconciliation bankReconciliation1 = new BankReconciliation();
        bankReconciliation1.init(list.get(0));
        YtsContext.setYtsContext("oldbankReconciliationObject", SerializationUtils.clone(bankReconciliation1));

        Short associatedStates = 1;//关联状态
        /*bankReconciliation1.setAssociationstatus(associatedStates);*/
        bankReconciliation1.setAutoassociation(corrData.getAuto());
        //智能对账：生单关联时，添加勾兑码
        bankReconciliation1.setSmartcheckno(corrData.getSmartcheckno());
        bankReconciliation1.setEntityStatus(EntityStatus.Update);
        CommonSaveUtils.updateBankReconciliation(bankReconciliation1);

        //自动关联确认操作
        //查询自动化参数配置表
        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
        QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("accentity").eq(bankReconciliation1.getAccentity()));
        querySchema1.addCondition(group1);
        List<AutoConfig> config = MetaDaoHelper.query(AutoConfig.ENTITY_NAME, querySchema1);
        if (config.size() > 0 && CollectionUtils.isNotEmpty(config)) {
            AutoConfig autoConfig = new AutoConfig();
            autoConfig.init(config.get(0));
            //判断“自动关联后确认”字段值
            if (autoConfig.getAutoassociateconfirm() != null && autoConfig.getAutoassociateconfirm()) {
                //查询对账单关联表数据
                QuerySchema querySchema2 = QuerySchema.create().addSelect("*");
                QueryConditionGroup group2 = QueryConditionGroup.and(
                        QueryCondition.name("bankreconciliation").eq(corrData.getBankReconciliationId()),
                        QueryCondition.name("billcode").eq(corrData.getCode()));
                querySchema2.addCondition(group2);
                List<BankReconciliationbusrelation_b> bankReconciliations = MetaDaoHelper.query(BankReconciliationbusrelation_b.ENTITY_NAME, querySchema2);
                if (bankReconciliations.size() > 0 && CollectionUtils.isNotEmpty(bankReconciliations)) {
                    List<BankReconciliationbusrelation_b> newBankReconciliations = new ArrayList<>();
                    for (int i = 0; i < bankReconciliations.size(); i++) {
                        BankReconciliationbusrelation_b bankReconciliationbusrelation_b = new BankReconciliationbusrelation_b();
                        bankReconciliationbusrelation_b.init(bankReconciliations.get(i));
                        newBankReconciliations.add(bankReconciliationbusrelation_b);
                    }
                    //获取自动关联确认接口参数
                    List corrIds = newBankReconciliations.stream().map(BankReconciliationbusrelation_b::getId).collect(Collectors.toList());
                    List dcFlags = new ArrayList();
                    dcFlags.add(corrData.getDcFlag());
                    //调用自动关联确认接口
                    corrOperationService.confirmCorrOpration(corrIds, dcFlags);
                }
            }
        }

    }

    /**
     * 回写付款单数据
     *
     * @param corrData
     */
    public void reWritePayMentData(CorrDataEntity corrData) throws Exception {
        Short associationStatus = 1;
        try {
            List<FundPayment_b> fundPaymentList = new ArrayList<FundPayment_b>();
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(corrData.getBusid()));
            querySchema.addCondition(group);
            fundPaymentList = MetaDaoHelper.query(FundPayment_b.ENTITY_NAME, querySchema);
            if (fundPaymentList != null && fundPaymentList.size() > 0) {
                FundPayment_b fundPayment = new FundPayment_b();
                fundPayment.init(fundPaymentList.get(0));
                YtsContext.setYtsContext("oldbizObjectB", SerializationUtils.clone(fundPayment));
                //更新主表pubts
                List<FundPayment> fundPayments = new ArrayList<FundPayment>();
                QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("id").eq(fundPayment.getMainid()));
                querySchema1.addCondition(group1);
                fundPayments = MetaDaoHelper.query(FundPayment.ENTITY_NAME, querySchema1);
                FundPayment ft = new FundPayment();
                ft.init(fundPayments.get(0));
                YtsContext.setYtsContext("oldbizObject", SerializationUtils.clone(ft));
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
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102251"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00063", "回写资金付款单错误：") /* "回写资金付款单错误：" */ + e.getMessage());
        }
    }

    /**
     * 回写收款单数据
     *
     * @param corrData
     */
    public void reWriteFundCollectionData(CorrDataEntity corrData) throws Exception {
        Short associationStatus = 1;
        try {
            List<FundCollection_b> fundCollectionList = new ArrayList<FundCollection_b>();
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(corrData.getBusid()));
            querySchema.addCondition(group);
            fundCollectionList = MetaDaoHelper.query(FundCollection_b.ENTITY_NAME, querySchema);
            if (fundCollectionList != null && fundCollectionList.size() > 0) {
                FundCollection_b fundCollection = new FundCollection_b();
                fundCollection.init(fundCollectionList.get(0));
                YtsContext.setYtsContext("oldbizObjectB", SerializationUtils.clone(fundCollection));
                //更新主表pubts
                List<FundCollection> fundCollections = new ArrayList<FundCollection>();
                QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                QueryConditionGroup group1 = QueryConditionGroup.and(QueryCondition.name("id").eq(fundCollection.getMainid()));
                querySchema1.addCondition(group1);
                fundCollections = MetaDaoHelper.query(FundCollection.ENTITY_NAME, querySchema1);
                FundCollection fc = new FundCollection();
                fc.init(fundCollections.get(0));
                YtsContext.setYtsContext("oldbizObject", SerializationUtils.clone(fc));
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
                    BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, corrData.getBankReconciliationId());
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
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102252"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00067", "回写资金收款单错误：") /* "回写资金收款单错误：" */ + e.getMessage());
        }
    }


    /**
     * * 为银行对账单的子表添加数据
     *
     * @param b
     * @param corrData
     * @return
     */
    public BankReconciliationbusrelation_b setBankData(BankReconciliationbusrelation_b b, CorrDataEntity corrData) {
        //赋值业务单据
        short billtype;
        if (corrData.getBillType().equals(BussDocumentType.fundcollection.getValue() + "")) {
            billtype = EventType.FundCollection.getValue();
        } else if (corrData.getBillType().equals(BussDocumentType.fundpayment.getValue() + "")) {
            billtype = EventType.FundPayment.getValue();
        } else if (corrData.getBillType().equals(BussDocumentType.transferaccount.getValue() + "")) {
            billtype = EventType.TransferAccount.getValue();
        } else {
            billtype = EventType.StwbSettleMentDetails.getValue();
        }
        Short isSur = corrData.getAuto() ? Relationstatus.Confirm.getValue() : Relationstatus.Confirmed.getValue();
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
        b.setBilltype(billtype);
        b.setDept(corrData.getDept());
        b.setProject(corrData.getProject());
        //关联状态
        b.setRelationstatus(isSur);
        return b;
    }

    @Override
    public void autoCorrCancel(Map paramMap) throws Exception {
        //装填需回滚数据
        BizObject oldbizObjectB = (BizObject) YtsContext.getYtsContext("oldbizObjectB");
        BizObject oldbizObject = (BizObject) YtsContext.getYtsContext("oldbizObject");
        if (oldbizObject != null) {
            oldbizObject.setEntityStatus(EntityStatus.Update);
            String ENTITY_NAME = null;
            String ENTITY_NAMEB = null;
            if ("0".equals(paramMap.get("billType"))) {
                ENTITY_NAME = FundCollection.ENTITY_NAME;
                ENTITY_NAMEB = FundCollection_b.ENTITY_NAME;
            } else if ("1".equals(paramMap.get("billType"))) {
                ENTITY_NAME = FundPayment.ENTITY_NAME;
                ENTITY_NAMEB = FundCollection_b.ENTITY_NAME;
            }

            oldbizObject.setPubts(MetaDaoHelper.findById(ENTITY_NAME, oldbizObject.getId()).get("pubts"));
            oldbizObjectB.setPubts(MetaDaoHelper.findById(ENTITY_NAMEB, oldbizObjectB.getId()).get("pubts"));

            MetaDaoHelper.delete(ENTITY_NAME, (Long) oldbizObject.get("id"));
            CmpMetaDaoHelper.insert(ENTITY_NAME, oldbizObject);
            MetaDaoHelper.delete(ENTITY_NAMEB, (Long) oldbizObjectB.get("id"));
            CmpMetaDaoHelper.insert(ENTITY_NAMEB, oldbizObjectB);
        }
        BizObject oldbankReconciliationObject = (BizObject) YtsContext.getYtsContext("oldbankReconciliationObject");
        if (oldbankReconciliationObject != null) {
            oldbankReconciliationObject.setPubts(MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, oldbankReconciliationObject.getId()).get("pubts"));
            Map<String, Object> bankAccount2 = QueryBaseDocUtils.queryEnterpriseBankAccountById(oldbankReconciliationObject.get("bankaccount"));
            oldbankReconciliationObject.set("banktype", bankAccount2.get("bank").toString());
            CommonBankReconciliationProcessor.HandlerReconciliationBeforeDeleteById((Long) oldbankReconciliationObject.get("id"),ymsJdbcApi);
            MetaDaoHelper.delete(BankReconciliation.ENTITY_NAME, (Long) oldbankReconciliationObject.get("id"));
            CommonSaveUtils.saveBankReconciliation(oldbankReconciliationObject);
        }
        Object bankReconciliationbusrelationBId = YtsContext.getYtsContext("bankReconciliationbusrelationBId");
        if (null == bankReconciliationbusrelationBId) {
            BizObject bankReconciliationbusrelationB = (BizObject) YtsContext.getYtsContext("bankReconciliationbusrelationB");
            if (null != bankReconciliationbusrelationB) {
                bankReconciliationbusrelationB.setPubts(MetaDaoHelper.findById(BankReconciliationbusrelation_b.ENTITY_NAME, bankReconciliationbusrelationB.getId()).get("pubts"));

                //MetaDaoHelper.delete(BankReconciliationbusrelation_b.ENTITY_NAME, (Long) bankReconciliationbusrelationB.get("id"));
                List<Long> relationIds = new ArrayList<>();
                relationIds.add((Long) bankReconciliationbusrelationB.get("id"));
                CommonSaveUtils.batchDeleteBankReconciliationbusrelationByIds(relationIds);
                //CmpMetaDaoHelper.insert(BankReconciliationbusrelation_b.ENTITY_NAME, bankReconciliationbusrelationB);
                List<BizObject> relationList = new ArrayList<>();
                relationList.add(bankReconciliationbusrelationB);
                CommonSaveUtils.insertBankReconciliationbusrelation_b(relationList);
            }
        } else {
            //MetaDaoHelper.delete(BankReconciliationbusrelation_b.ENTITY_NAME, (Long) bankReconciliationbusrelationBId);
            List<Long> relationIds = new ArrayList<>();
            relationIds.add((Long) bankReconciliationbusrelationBId);
            CommonSaveUtils.batchDeleteBankReconciliationbusrelationByIds(relationIds);
        }
    }

    @Override
    public Map<Long, Long> businessGenerateFundNew(List<Long> bankreconciliationIds) throws Exception {
        Map<Long, Long> idRelation = new HashMap<>();
        if (null == bankreconciliationIds || bankreconciliationIds.size() == 0) return idRelation;
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").in(bankreconciliationIds));
        querySchema.addCondition(group);
        List<Map<String, Object>> bankreconciliationMapList = MetaDaoHelper.query(BankReconciliation.ENTITY_NAME, querySchema);
        if (null != bankreconciliationMapList && bankreconciliationMapList.size() > 0) {
            List<BankReconciliation> bankreconciliationList = new ArrayList<>();
            for (Map<String, Object> bankreconciliationMap : bankreconciliationMapList) {
                BankReconciliation bankReconciliation = new BankReconciliation();
                bankReconciliation.init(bankreconciliationMap);
                if (bankReconciliation.getDc_flag().equals(Direction.Debit)) {
                    bankReconciliation.setGenertbilltype(EventType.FundPayment.getValue());
                } else if (bankReconciliation.getDc_flag().equals(Direction.Credit)) {
                    bankReconciliation.setGenertbilltype(EventType.FundCollection.getValue());
                }
                bankreconciliationList.add(bankReconciliation);
            }
            idRelation = bankreconciliationGenerateDoc(bankreconciliationList);
        }
        return idRelation;
    }

    /**
     * 1,根据autoorderrule判断生成收款单或付款单 -- 枚举 EventType
     * 2,调用收付款单不同的生单Service
     *
     * @param bankReconciliations
     * @return
     */
    private Map<Long, Long> bankreconciliationGenerateDoc(List<BankReconciliation> bankReconciliations) throws Exception {
        //筛选资金付款单 TODO 筛选出不同的单据类型
        List<BankReconciliation> fundPayMent = bankReconciliations.stream()
                .filter(b -> null != b.getGenertbilltype() && EventType.FundPayment.getValue() == b.getGenertbilltype())
                .collect(Collectors.toList());
        //筛选资金收款单
        List<BankReconciliation> fundCollection = bankReconciliations.stream()
                .filter(b -> null != b.getGenertbilltype() && EventType.FundCollection.getValue() == b.getGenertbilltype())
                .collect(Collectors.toList());
        Map<Long, Long> idRelation = new HashMap<>();
        if (fundPayMent != null && fundPayMent.size() > 0) {
            //生成资金付款单
            idRelation = bankreconciliationGenerateFundpayment(fundPayMent);
            //修改生单状态
//            updateIsautocreatebill(fundPayMent);
            //自动确认
            autoConfirmBankreconciliation(fundPayMent);
        }
        if (fundCollection != null && fundCollection.size() > 0) {
            //生成资金收款单
            idRelation = bankReconciliationGenerateFundCollection(fundCollection);
            //修改生单状态
//            updateIsautocreatebill(fundCollection);
            //自动确认
            autoConfirmBankreconciliation(fundCollection);
        }
        return idRelation;
    }

    /**
     * 1,根据转单规则生成资金收款单
     * 2,回写业务生单信息
     *
     * @param bankReconciliations
     * @return
     */
    private Map<Long, Long> bankReconciliationGenerateFundCollection(List<BankReconciliation> bankReconciliations) throws Exception {
        List<CorrDataEntity> corrDataEntities = new ArrayList<CorrDataEntity>();//需要回写关联信息实体
        List<FundCollection> fundCollections = new ArrayList<FundCollection>();
        Map<Long, Long> idRelation = new HashMap<>();
        /**
         * 1，数据转换为可保存的收款单实体
         * 2，拼接需要回写的关联信息实体
         */
        for (BankReconciliation bankReconciliation : bankReconciliations) {
            Map<String, Object> map = generateFundCollection(bankReconciliation, "pull");
            List<Map<String, Object>> tarList = (List) map.get("tarList");//生成资金付款单数据列表
            if (tarList.isEmpty()) continue;
            FundCollection fundCollection = getFundCollection(tarList.get(0));
            fundCollections.add(fundCollection);
            fundCollection.setCreateDate(new Date());
            fundCollection.setCreateTime(new Date());
            fundCollection.setCreator(InvocationInfoProxy.getUsername());
            fundCollection.setUserId(InvocationInfoProxy.getUserid());
            //结算简强字段赋值
            fundCommonService.setSimpleSettleValue(IBillNumConstant.FUND_COLLECTION, fundCollection);
            corrDataEntities.add(getCorrDataEntity(fundCollection));
            idRelation.put(bankReconciliation.getId(), fundCollection.getId());
        }
        CmpMetaDaoHelper.insert(FundCollection.ENTITY_NAME, fundCollections);//保存资金收款单信息
        List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = batchReWriteBankRecilicationCorrelation(corrDataEntities);//回写关联信息
        for (BankReconciliation bankReconciliation : bankReconciliations) {
            List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bElem = new ArrayList<>();//对账单关联实体
            for (BankReconciliationbusrelation_b bankReconciliationbusrelation_b : bankReconciliationbusrelation_bs) {
                if (bankReconciliationbusrelation_b.getBankreconciliation().equals(bankReconciliation.getId())) {
                    bankReconciliationbusrelation_b.setEntityStatus(EntityStatus.Update);
                    bankReconciliationbusrelation_bElem.add(bankReconciliationbusrelation_b);
                }
            }
            bankReconciliation.setBankReconciliationbusrelation_b(bankReconciliationbusrelation_bElem);
        }
        return idRelation;
    }

    private List<BankReconciliationbusrelation_b> batchReWriteBankRecilicationCorrelation(List<CorrDataEntity> corrDataEntities) throws Exception {
        List<Long> ids = new ArrayList<Long>();//存储对账单id
        List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = new ArrayList<BankReconciliationbusrelation_b>();//对账单关联实体
        if (corrDataEntities.isEmpty()) return null; //需回写关联数据为空.
        //用来封装智能对账勾兑码，存储单据ID和对应的智能对账码
        List<CtmJSONObject> smartCheckResult = new ArrayList<>();
        //智能对账,关联数据添加勾兑码
//        String smartcheckno = UUID.randomUUID().toString().replace("-", "");
        //调用资金结算财资统一对账码接口生成
        String smartcheckno = RemoteDubbo.get(ReconciliateCodeGenerator.class, IDomainConstant.MDD_DOMAIN_STWB).generate();

        if (corrDataEntities.get(0).getBillClaimItemId() != null) {
            if (!checkPubts(corrDataEntities.get(0)))
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102253"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00066", "当前数据已被修改") /* "当前数据已被修改" */);

            //认领单回写
            YtsContext.setYtsContext("rollbackbillclaimid", corrDataEntities.get(0).getBillClaimItemId());//装填需回滚数据
            reWriteBusCorrDataService.reWriteBillClaimData(corrDataEntities.get(0).getBillClaimItemId(),
                    AssociationStatus.Associated.getValue(), corrDataEntities.get(0).getSmartcheckno(),
                    ClaimCompleteType.RecePayGen.getValue());
            //组装需回写对账单数据
            List<CorrDataEntity> corrDatas = new ArrayList<CorrDataEntity>();
            List<BillClaimItem> list;
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(corrDataEntities.get(0).getBillClaimItemId()));
            querySchema.addCondition(group);
            list = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema, null);
            if (list != null && list.size() > 0) {
                for (BillClaimItem billClaimItem : list) {//循环取出所有对账单id，拼装后进行回写
                    CorrDataEntity corrData = new CorrDataEntity();
                    BeanUtils.copyProperties(corrDataEntities.get(0), corrData);
                    List<BankReconciliation> bankReconciliations;
                    corrData.setBankReconciliationId(billClaimItem.getBankbill());
                    corrData.setAuto(false);
                    corrDatas.add(corrData);
                }
            }
            corrDataEntities = corrDatas;
        }

        Boolean isAuto = corrDataEntities.get(0).getAuto();
        Map pubtsmap = new HashMap();
        for (CorrDataEntity corr : corrDataEntities) {
            //组装关联实体
            bankReconciliationbusrelation_bs.add(getBankReconciliationbusrelation_b(corr));
            /**
             * 写入需判断对账单的pubts
             * 1，如为认领单不需要校验对账单pubts
             * 2，资金收付款单不需要校验对账单pubts
             */
            if (corr.getBillClaimItemId() == null && !Short.valueOf(corr.getBillType()).equals(EventType.FundPayment.getValue()) && !Short.valueOf(corr.getBillType()).equals(EventType.FundCollection.getValue())) {
                pubtsmap.put(corr.getBankReconciliationId(), corr.getBankReconciliationPubts());
            }
            //写入对账单id
            ids.add(corr.getBankReconciliationId());
        }
        //查询对账单
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        QuerySchema querySchema2 = QuerySchema.create().addSelect("*");
        QueryConditionGroup group2 = QueryConditionGroup.and(QueryCondition.name("id").in(ids));
        querySchema2.addCondition(group2);
        List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema2, null);
        for (int i = 0; i < bankReconciliations.size(); i++) {
            Date date = bankReconciliations.get(i).getPubts();
            String pus = sf.format(date);
            //处理银行对账单前，对单据进行判断是否为最新状态
            if (pubtsmap.get(bankReconciliations.get(i).getId()) != null && !pubtsmap.get(bankReconciliations.get(i).getId()).equals(pus)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102254"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00069", "单据已被修改") /* "单据已被修改" */);
            }
//            if(bankReconciliations.get(i).getRelationstatus()!=null){//已被自动生单
//                throw new CtmException("单据已被修改");
//            }
            if (isAuto) {//自动生单 - 待确认   手动生单 - 已确认
                bankReconciliations.get(i).setRelationstatus(Relationstatus.Confirm.getValue());
            } else {
                bankReconciliations.get(i).setRelationstatus(Relationstatus.Confirmed.getValue());
            }
            bankReconciliations.get(i).setAutoassociation(corrDataEntities.get(0).getAuto());
            /*bankReconciliations.get(i).setAssociationstatus(AssociationStatus.Associated.getValue());*/
            Short associationcount = bankReconciliations.get(i).getAssociationcount();
            //关联次数不为空的时候直接将关联次数设置为2
            if (null != associationcount) {
                bankReconciliations.get(i).setAssociationcount(AssociationCount.Second.getValue());
            } else {
                bankReconciliations.get(i).setAssociationcount(AssociationCount.First.getValue());
            }
            bankReconciliations.get(i).setIsautocreatebill(true);
            bankReconciliations.get(i).setEntityStatus(EntityStatus.Update);
        }
        CommonSaveUtils.updateBankReconciliation(bankReconciliations);//回写银行对账单数据
        YtsContext.setYtsContext("bankReconciliationIds", ids);//装填需回滚数据
        //CmpMetaDaoHelper.insert(BankReconciliationbusrelation_b.ENTITY_NAME, bankReconciliationbusrelation_bs);//写入关联信息
        CommonSaveUtils.insertBankReconciliationbusrelation_b(bankReconciliationbusrelation_bs);
        YtsContext.setYtsContext("batchCorrData", bankReconciliationbusrelation_bs);//装填需回滚数据
        List corrDataIds = new ArrayList<>();
        bankReconciliationbusrelation_bs.stream().forEach(e -> {
            corrDataIds.add(e.getId());
        });
        YtsContext.setYtsContext("corrDataIds", corrDataIds);//装填需回滚数据


        return bankReconciliationbusrelation_bs;
    }

    private BankReconciliationbusrelation_b getBankReconciliationbusrelation_b(CorrDataEntity corr) {
        BankReconciliationbusrelation_b bankReconciliationbusrelation_b = new BankReconciliationbusrelation_b();
        bankReconciliationbusrelation_b.setAccentity(corr.getAccentity());
        bankReconciliationbusrelation_b.setBillnum(corr.getBillNum());
        bankReconciliationbusrelation_b.setBillid(corr.getBusid());
        bankReconciliationbusrelation_b.setBilltype(Short.valueOf(corr.getBillType()));
        bankReconciliationbusrelation_b.setAmountmoney(corr.getOriSum());
        bankReconciliationbusrelation_b.setBankreconciliation(corr.getBankReconciliationId());
        bankReconciliationbusrelation_b.setBillcode(corr.getCode());
        bankReconciliationbusrelation_b.setDept(corr.getDept());
        bankReconciliationbusrelation_b.setProject(corr.getProject());
        if (corr.getAuto()) {
            bankReconciliationbusrelation_b.setRelationstatus(Relationstatus.Confirm.getValue());
        } else {
            bankReconciliationbusrelation_b.setRelationstatus(Relationstatus.Confirmed.getValue());
        }
        bankReconciliationbusrelation_b.setRelationtype(Relationtype.MakeBillAssociated.getValue());
        bankReconciliationbusrelation_b.setVouchdate(corr.getVouchdate());
        bankReconciliationbusrelation_b.setSrcbillid(corr.getMainid());
        bankReconciliationbusrelation_b.setId(ymsOidGenerator.nextId());
        bankReconciliationbusrelation_b.setEntityStatus(EntityStatus.Insert);
        return bankReconciliationbusrelation_b;
    }

    private boolean checkPubts(CorrDataEntity corr) throws Exception {
        if (corr.getBillClaimItemId() == null) return true;
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<BillClaim> list;
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").eq(corr.getBillClaimItemId()));
        querySchema.addCondition(group);
        list = MetaDaoHelper.queryObject(BillClaim.ENTITY_NAME, querySchema, null);
        Date date = list.get(0).getPubts();
        String pus = sf.format(date);
        if (!pus.equals(corr.getBankReconciliationPubts())) {
            return false;
        }
        return true;
    }

    /**
     * 生成资金付款单单据
     * 返回已生成的单据用于回写信息
     *
     * @param bankReconciliation
     * @return
     */
    public Map<String, Object> generateFundCollection(BankReconciliation bankReconciliation, String MAKETYPE) throws Exception {
        PushAndPullVO pullVO = forPushAndPullVOValue(bankReconciliation, "banktofundcollection", "pull");//获取转单实体
        Map<String, Object> map = makeBillRuleClientService.getTargetList(pullVO, MAKETYPE);//调用转单规则方法 - 返回转单成功的单据
        return map;
    }

    /**
     * 获取转单规则实体
     *
     * @param bankReconciliation
     * @return
     */
    private PushAndPullVO forPushAndPullVOValue(BankReconciliation bankReconciliation, String FUNDCOLLECTIONRULE, String MAKETYPE) {
        List<BizObject> bizObjects = new ArrayList<BizObject>();
        List<String> ids = new ArrayList<String>();
        /**
         * 将对账单实体转成BizObject
         * 添加参数 -- 款项类型&交易类型
         * 将单据id放入ids
         */
        BizObject data = bankReconciliation;
        bizObjects.add(data);
        ids.add(bankReconciliation.getId().toString());
        PushAndPullVO pullVO = new PushAndPullVO();
        pullVO.setCode(FUNDCOLLECTIONRULE);
        pullVO.setMakeType(MAKETYPE);
        pullVO.setChildIds(ids);
        pullVO.setData(bizObjects);
        pullVO.setIsMainSelect(1);
        return pullVO;
    }

    /**
     * 获取业务生单 - 资金收款单对象
     *
     * @param obj
     * @return
     */
    private FundCollection getFundCollection(Map<String, Object> obj) throws Exception {
        FundCollection fundCollection = new FundCollection();
        fundCollection.init(obj);
        fundCollection.setFundCollection_b(getFundCollection_b(obj));
        fundCollection.setEntityStatus(EntityStatus.Insert);
        fundCollection.setOriSum(fundCollection.getOriSum().setScale(8, BigDecimal.ROUND_DOWN));
        fundCollection.setId(ymsOidGenerator.nextId());
        fundCollection.setVerifystate(VerifyState.INIT_NEW_OPEN.getValue());
        fundCollection.setVoucherstatus(VoucherStatus.Empty);
        fundCollection.FundCollection_b().get(0).setMainid(String.valueOf((long)fundCollection.getId()));
        fundCollection.setNatSum(fundCollection.getNatSum().setScale(8, BigDecimal.ROUND_DOWN));
        fundCollection.setConfirmstatus(Relationstatus.Confirm.getValue());//待确认状态
        //fundCollection.setTradetype(autoorderrule_b.getTradetype());
        fundCollection.setCode(getBillCode(fundCollection));
        // 特征处理 否则保存报错
        BizObject characterDef = fundCollection.get("characterDef");
        if (characterDef != null) {
            characterDef.put("id", ymsOidGenerator.nextId());
            characterDef.put("ytenant", fundCollection.get("ytenantId"));
        }
        // 设置创建人
        if (null == fundCollection.getCreator()) {
            fundCollection.setCreator(AppContext.getCurrentUser().getName());
        }
        if (null == fundCollection.getCreatorId()) {
            fundCollection.setCreatorId(AppContext.getCurrentUser().getId());
        }
        // 设置创建时间
        if (null == fundCollection.getCreateDate()) {
            fundCollection.setCreateDate(new Date());
        }
        if (null == fundCollection.getCreateTime()) {
            fundCollection.setCreateTime(new Date());
        }
        if (null == fundCollection.getIsWfControlled()) {
            ProcessDefinitionQueryParam param = new ProcessDefinitionQueryParam();
            param.setCategory("");
            param.setBillTypeId(ICmpConstant.CM_CMP_FUND_COLLECTION);
            param.setOrgId(fundCollection.getAccentity());
            RepositoryService repositoryService = processService.bpmRestServices().getRepositoryService();
            Object result = repositoryService.checkProcessDefinition(param);
            if (!((ObjectNode) result).get("hasProcessDefinition").booleanValue()) {
                fundCollection.setIsWfControlled(false);
            } else {
                fundCollection.setIsWfControlled(true);
            }
        }
        if (null == fundCollection.getTradetype()) {
            fundCollection.setTradetype(getDefultTradeType(ICmpConstant.CM_CMP_FUND_COLLECTION));
        }
        if (null == fundCollection.getIsEnabledBsd()) {
            fundCollection.setIsEnabledBsd(false);
        }
        return fundCollection;
    }

    /**
     * 获取默认的交易类型
     *
     * @return
     */
    private String getDefultTradeType(String formId) {
        String billTypeId = null;
        try {
            BillContext bc = new BillContext();
            bc.setFullname("bd.bill.BillTypeVO");
            bc.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
            QuerySchema schema = QuerySchema.create();
            schema.addSelect("id");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("form_id").eq(formId));
            schema.addCondition(conditionGroup);
            List<Map<String, Object>> list = MetaDaoHelper.query(bc, schema);
            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(list)) {
                Map<String, Object> objectMap = list.get(0);
                if (!ValueUtils.isNotEmptyObj(objectMap)) {
                    log.error("查询资金付款单交易类型失败！请检查数据！");
//                    throw new CtmException("查询资金付款单交易类型失败！请检查数据。");
                }
                billTypeId = MapUtils.getString(objectMap, "id");
            }
            Map<String, Object> tradetypeMap = cmCommonService.queryTransTypeById(billTypeId, "1", null);
            if (ValueUtils.isNotEmptyObj(tradetypeMap)) {
                return tradetypeMap.get("id").toString();
            }
        } catch (Exception e) {
            log.error("未获取到默认的交易类型！, billTypeId = {}, e = {}", billTypeId, e.getMessage());
//            throw new CtmException("未获取到默认的交易类型！");
        }
        return null;
    }

    /**
     * 获取业务生单 - 收款单子表对象
     *
     * @param obj
     * @return
     */
    private List<FundCollection_b> getFundCollection_b(Map<String, Object> obj) {
        List<Map<String, Object>> fundCollection_bs = (List<Map<String, Object>>) obj.get("FundCollection_b");
        List<FundCollection_b> fundCollection_bList = new ArrayList<FundCollection_b>();
        FundCollection_b fundCollection_b = new FundCollection_b();
        fundCollection_b.init(fundCollection_bs.get(0));
        fundCollection_b.setNatSum(fundCollection_b.getNatSum().setScale(8, BigDecimal.ROUND_DOWN));
        fundCollection_b.setOriSum(fundCollection_b.getOriSum().setScale(8, BigDecimal.ROUND_DOWN));
        fundCollection_b.setSettlesuccessSum(fundCollection_b.getNatSum().setScale(8, BigDecimal.ROUND_DOWN));
        //fundCollection_b.setQuickType(autoorderrule_b.getQuickType());
        fundCollection_b.setId(ymsOidGenerator.nextId());
        fundCollection_b.setBankReconciliationId(fundCollection_b.get("bankReconciliationId").toString());
        fundCollection_b.setAssociationStatus(AssociationStatus.Associated.getValue());
        fundCollection_b.setEntityStatus(EntityStatus.Insert);

        // 特征处理 否则保存报错
        BizObject characterDefb = fundCollection_b.get("characterDefb");
        if (characterDefb != null) {
            characterDefb.put("id", ymsOidGenerator.nextId());
            characterDefb.put("ytenant", fundCollection_b.get("ytenantId"));
        }

        fundCollection_bList.add(fundCollection_b);
        return fundCollection_bList;
    }


    /**
     * 生成关联关系实体
     *
     * @param fundCollection
     * @return
     */
    private CorrDataEntity getCorrDataEntity(FundCollection fundCollection) {
        CorrDataEntity corrData = new CorrDataEntity();
        FundCollection_b fundCollection_b = fundCollection.FundCollection_b().get(0);
        corrData.setBankReconciliationId(Long.valueOf(fundCollection_b.getBankReconciliationId()));
        corrData.setAccentity(fundCollection.getAccentity());
        corrData.setCode(fundCollection.getCode());
        corrData.setAuto(true);
        corrData.setBillNum(IBillNumConstant.FUND_COLLECTION);
        corrData.setDept(fundCollection_b.getDept());
        corrData.setBillType(String.valueOf(EventType.FundCollection.getValue()));
        corrData.setBusid(fundCollection_b.getId());
        corrData.setMainid(fundCollection.getId());
        corrData.setOriSum(fundCollection_b.getOriSum());
        corrData.setVouchdate(fundCollection.getVouchdate());
        corrData.setProject(fundCollection_b.getProject());
        return corrData;
    }

    /**
     * 获取单据编码
     *
     * @param fundCollection
     * @return
     */
    private String getBillCode(FundCollection fundCollection) {
        IBillCodeComponentService billCodeComponentService = AppContext.getBean(IBillCodeComponentService.class);
        BillCodeComponentParam billCodeComponentParam = new BillCodeComponentParam(CmpBillCodeMappingConfUtils.getBillCode(IBillNumConstant.FUND_COLLECTION), IBillNumConstant.FUND_COLLECTION, fundCollection.get("ytenantId"), null, null, new BillCodeObj[]{new BillCodeObj(fundCollection)});
        String[] codelist = billCodeComponentService.getBatchBillCodes(billCodeComponentParam);
        if (codelist != null && codelist.length > 0) {
            return codelist[0];
        } else {
            return null;
        }
    }

    private void autoConfirmBankreconciliation(List<BankReconciliation> pushData) throws Exception {
        try {
            //遍历对账单列表,调用自动生单确认接口
            for (BankReconciliation bankReconciliation : pushData) {
                //调用自动生单确认接口
                JsonNode params = JSONBuilderUtils.beanToJson(bankReconciliation);
                ObjectNode paramsObject = (ObjectNode) params;
                List<Map<String, Object>> bankreconciliation_blist = JSONBuilderUtils.jsonListToMapList(JSONBuilderUtils.jsonToList(params.get("BankReconciliationbusrelation_b")));
                ;
                if (null != bankreconciliation_blist && bankreconciliation_blist.size() > 0) {
                    for (Map<String, Object> bankreconciliation_b : bankreconciliation_blist) {
                        paramsObject.put("autocreatebillcode", bankreconciliation_b.get("billcode").toString());
                    }
                }
                bankAutoPushBillService.confirmBill(paramsObject);
            }
        } catch (Exception e) {
            log.error("自动生单确认异常" + e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102255"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00065", "资金结算单据生单异常:") /* "资金结算单据生单异常:" */ + e.getMessage());
        }

    }

    /**
     * 1，自动生单 -- 银行对账单生成资金付款单
     * 2，生单成功后，回调关联接口 - 回写生单关联信息
     *
     * @param bankReconciliations
     * @return
     */
    private Map<Long, Long> bankreconciliationGenerateFundpayment(List<BankReconciliation> bankReconciliations) throws Exception {
        List<CorrDataEntity> corrDataEntities = new ArrayList<>();//需要回写关联信息实体
        List<FundPayment> fundPayments = new ArrayList<>();
        Map<Long, Long> idRelation = new HashMap<>();

        /**
         * 1，转换数据格式为可保存付款单实体
         * 2，拼接回写关联信息实体
         */
        for (BankReconciliation bankReconciliation : bankReconciliations) {
            Map<String, Object> map = generateFundpayment(bankReconciliation);
            List<Map<String, Object>> tarList = (List) map.get("tarList");//生成资金付款单数据列表
            if (tarList.isEmpty()) continue;
            FundPayment fundPayment = getFundPayment(tarList.get(0));
            fundPayment.setCreateDate(new Date());
            fundPayment.setCreateTime(new Date());
            fundPayment.setCreator(InvocationInfoProxy.getUsername());
            fundPayment.setUserId(InvocationInfoProxy.getUserid());
            //结算简强字段赋值
            fundCommonService.setSimpleSettleValue(IBillNumConstant.FUND_PAYMENT, fundPayment);
            fundPayments.add(fundPayment);
            corrDataEntities.add(getCorrDataEntity(fundPayment, fundPayment.FundPayment_b().get(0)));
            idRelation.put(bankReconciliation.getId(), fundPayment.getId());
        }
        CmpMetaDaoHelper.insert(FundPayment.ENTITY_NAME, fundPayments);//保存资金付款单主表信息
        List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bs = batchReWriteBankRecilicationCorrelation(corrDataEntities);//回写关联信息
        for (BankReconciliation bankReconciliation : bankReconciliations) {
            List<BankReconciliationbusrelation_b> bankReconciliationbusrelation_bElem = new ArrayList<>();//对账单关联实体
            for (BankReconciliationbusrelation_b bankReconciliationbusrelation_b : bankReconciliationbusrelation_bs) {
                if (bankReconciliationbusrelation_b.getBankreconciliation().equals(bankReconciliation.getId())) {
                    bankReconciliationbusrelation_b.setEntityStatus(EntityStatus.Update);
                    bankReconciliationbusrelation_bElem.add(bankReconciliationbusrelation_b);
                }
            }
            bankReconciliation.setBankReconciliationbusrelation_b(bankReconciliationbusrelation_bElem);
        }
        return idRelation;
    }

    /**
     * 生成资金付款单单据
     * 返回已生成的单据用于回写信息
     *
     * @param bankReconciliation
     * @return
     */
    private Map<String, Object> generateFundpayment(BankReconciliation bankReconciliation) throws Exception {
        PushAndPullVO pullVO = forPushAndPullVOValue(bankReconciliation, "banktofundpayment", "push");//获取转单实体
        Map<String, Object> map = makeBillRuleClientService.getTargetList(pullVO, "push");//调用转单规则方法 - 返回转单成功的单据
        return map;
    }

    /**
     * 获取业务生单 - 资金付款单对象
     *
     * @param obj
     * @return
     */
    private FundPayment getFundPayment(Map<String, Object> obj) throws Exception {
        FundPayment fundPayment = new FundPayment();
        fundPayment.init(obj);
        fundPayment.setFundPayment_b(getFundPayment_b(obj));
        fundPayment.setId(ymsOidGenerator.nextId());
        fundPayment.FundPayment_b().get(0).setMainid(String.valueOf((long)fundPayment.getId()));
        fundPayment.setEntityStatus(EntityStatus.Insert);
        fundPayment.setConfirmstatus(Relationstatus.Confirm.getValue());//生成单据为待确认状态
        fundPayment.setNatSum(fundPayment.getNatSum().setScale(8, BigDecimal.ROUND_DOWN));//规范精度
        fundPayment.setOriSum(fundPayment.getOriSum().setScale(8, BigDecimal.ROUND_DOWN));
        fundPayment.setVerifystate(VerifyState.INIT_NEW_OPEN.getValue());
        fundPayment.setVoucherstatus(VoucherStatus.Empty);
        fundPayment.FundPayment_b().get(0).setMainid(String.valueOf((long)fundPayment.getId()));
        //fundPayment.setTradetype(autoorderrule_b.getTradetype());
        fundPayment.setCode(getBillCode(fundPayment));
        // 特征处理 否则保存报错
        BizObject characterDef = fundPayment.get("characterDef");
        if (characterDef != null) {
            characterDef.put("id", ymsOidGenerator.nextId());
            characterDef.put("ytenant", fundPayment.getYtenantId());
        }

        // 设置创建人
        if (null == fundPayment.getCreator()) {
            fundPayment.setCreator(AppContext.getCurrentUser().getName());
        }
        if (null == fundPayment.getCreatorId()) {
            fundPayment.setCreatorId(AppContext.getCurrentUser().getId());
        }
        // 设置创建时间
        if (null == fundPayment.getCreateDate()) {
            fundPayment.setCreateDate(new Date());
        }
        if (null == fundPayment.getCreateTime()) {
            fundPayment.setCreateTime(new Date());
        }
        if (null == fundPayment.getIsWfControlled()) {
            ProcessDefinitionQueryParam param = new ProcessDefinitionQueryParam();
            param.setCategory("");
            param.setBillTypeId(ICmpConstant.CM_CMP_FUND_PAYMENT);
            param.setOrgId(fundPayment.getAccentity());
            RepositoryService repositoryService = processService.bpmRestServices().getRepositoryService();
            Object result = repositoryService.checkProcessDefinition(param);
            if (!((ObjectNode) result).get("hasProcessDefinition").booleanValue()) {
                fundPayment.setIsWfControlled(false);
            } else {
                fundPayment.setIsWfControlled(true);
            }
        }
        if (null == fundPayment.getTradetype()) {
            fundPayment.setTradetype(getDefultTradeType(ICmpConstant.CM_CMP_FUND_PAYMENT));
        }
        if (null == fundPayment.getIsEnabledBsd()) {
            fundPayment.setIsEnabledBsd(false);
        }
        return fundPayment;
    }

    /**
     * 获取业务生单 - 付款单子表对象
     *
     * @param obj
     * @return
     */
    private List<FundPayment_b> getFundPayment_b(Map<String, Object> obj) throws Exception {
        List<Map<String, Object>> fundPayment_bs = (List<Map<String, Object>>) obj.get("FundPayment_b");
        List<FundPayment_b> fundPayment_bs1 = new ArrayList<FundPayment_b>();
        FundPayment_b fundPayment_b = new FundPayment_b();
        fundPayment_b.init(fundPayment_bs.get(0));
        fundPayment_b.setAssociationStatus(AssociationStatus.Associated.getValue());
        fundPayment_b.setNatSum(fundPayment_b.getNatSum().setScale(8, BigDecimal.ROUND_DOWN));
        fundPayment_b.setOriSum(fundPayment_b.getOriSum().setScale(8, BigDecimal.ROUND_DOWN));
        fundPayment_b.setSettlesuccessSum(fundPayment_b.getNatSum().setScale(8, BigDecimal.ROUND_DOWN));
        fundPayment_b.setBankReconciliationId(fundPayment_b.get("bankReconciliationId").toString());
        fundPayment_b.setSignature(getSign(fundPayment_b));
        //fundPayment_b.setQuickType(autoorderrule_b.getQuickType());
        fundPayment_b.setId(ymsOidGenerator.nextId());
        fundPayment_b.setEntityStatus(EntityStatus.Insert);
        // 特征处理 否则保存报错
        BizObject characterDefb = fundPayment_b.get("characterDefb");
        if (characterDefb != null) {
            characterDefb.put("id", ymsOidGenerator.nextId());
            characterDefb.put("ytenant", fundPayment_b.get("ytenantId"));
        }
        fundPayment_bs1.add(fundPayment_b);
        return fundPayment_bs1;
    }

    /**
     * 生成关联关系实体
     *
     * @param fundPayment
     * @param fundPayment_b
     * @return
     */
    private CorrDataEntity getCorrDataEntity(FundPayment fundPayment, FundPayment_b fundPayment_b) {
        CorrDataEntity corrData = new CorrDataEntity();
        corrData.setBankReconciliationId(Long.valueOf(fundPayment_b.getBankReconciliationId()));
        corrData.setAccentity(fundPayment.getAccentity());
        corrData.setCode(fundPayment.getCode());
        corrData.setAuto(true);
        corrData.setBillNum(IBillNumConstant.FUND_PAYMENT);
        corrData.setDept(fundPayment_b.getDept());
        corrData.setBillType(String.valueOf(EventType.FundPayment.getValue()));
        corrData.setBusid(fundPayment_b.getId());
        corrData.setMainid(fundPayment.getId());
        corrData.setOriSum(fundPayment_b.getOriSum());
        corrData.setVouchdate(fundPayment.getVouchdate());
        corrData.setProject(fundPayment_b.getProject());
        return corrData;
    }

    /**
     * 获取单据编码
     *
     * @param fundPayment
     * @return
     */
    private String getBillCode(FundPayment fundPayment) {
        IBillCodeComponentService billCodeComponentService = AppContext.getBean(IBillCodeComponentService.class);
        BillCodeComponentParam billCodeComponentParam = new BillCodeComponentParam(CmpBillCodeMappingConfUtils.getBillCode(IBillNumConstant.FUND_PAYMENT), IBillNumConstant.FUND_PAYMENT, fundPayment.getYtenantId(), null, null, new BillCodeObj[]{new BillCodeObj(fundPayment)});
        String[] codelist = billCodeComponentService.getBatchBillCodes(billCodeComponentParam);
        if (codelist != null && codelist.length > 0) {
            return codelist[0];
        } else {
            return null;
        }
    }

    /**
     * 获取付款单子表签名
     *
     * @param fundPayment_b
     * @return
     * @throws Exception
     */
    private String getSign(FundPayment_b fundPayment_b) throws Exception {
        DecimalFormat decimalFormat = new DecimalFormat("0.00#");
        String oppositeObjectName = fundPayment_b.getOppositeobjectname();
        String oppositeAccountName = fundPayment_b.getOppositeaccountname();
        String oriSum = decimalFormat.format(fundPayment_b.getOriSum());
        CtmJSONObject oriJson = new CtmJSONObject();
        oriJson.put("oppositeObjectName", oppositeObjectName);
        oriJson.put("oppositeAccountName", oppositeAccountName);
        oriJson.put(IBussinessConstant.ORI_SUM, oriSum);
        String signMsg = signatureService.chanPaySignMessage(oriJson.toString());
        return signMsg;
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public void batchQueryTransactionDetail(Map paramMap) throws Exception {
        YmsLock ymsLock = null;
        try {
            ymsLock = JedisLockUtils.lockBillWithOutTrace(paramMap.get("acct_no") + ICmpConstant.QUERYTRANSDETAILKEY);
            if (ymsLock == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102256"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CD","系统正在对此账户拉取中") /* "系统正在对此账户拉取中" */);
            }
            //这里统一接口参数 后台只调用一个方法
            BankLinkParam paramBankLink = new BankLinkParam();
            paramBankLink.setAccountId((String) paramMap.get("accountId"));
            paramBankLink.setAcct_no((String) paramMap.get("acct_no"));
            paramBankLink.setAcct_name((String) paramMap.get("acct_name"));
            paramBankLink.setCustomNo((String) paramMap.get("customNo")); // 客户号
            paramBankLink.setSignature((String) paramMap.get("signature"));; // 签名
            paramBankLink.setOperator((String) paramMap.get("operator")); // 操作人
            paramBankLink.setStartDate((String) paramMap.get("startDate")); ; // 开始日期
            paramBankLink.setEndDate((String) paramMap.get("endDate")); ; // 结束日期
            paramBankLink.setBegNum((Integer) paramMap.get("begNum")); ; // 开始数量
            bankDealDetailService.batchQueryTransactionDetailForRpc(paramBankLink);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Exception(e);
        }finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public void batchQueryTransactionDetailForRpc(BankLinkParam paramMap) throws Exception {
        YmsLock ymsLock = null;
        try {
            ymsLock = JedisLockUtils.lockBillWithOutTrace(paramMap.getAcct_no() + ICmpConstant.QUERYTRANSDETAILKEY);
            if (ymsLock == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102256"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806CD","系统正在对此账户拉取中") /* "系统正在对此账户拉取中" */);
            }
            bankDealDetailService.batchQueryTransactionDetailForRpc(paramMap);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Exception(e);
        }finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
    }

    @Override
    public void deleteBankReconciliationByAccountDetail(BankReconciliationEntityDto param) throws Exception {
        String accountid = param.getBankaccount();
        String bankseqno = param.getBank_seq_no();
        String direction = param.getDc_flag();
        Date tranTime = param.getTran_time();
        BigDecimal tran_amt = param.getTran_amt();
        BigDecimal acctbal = param.getAcctbal();
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QuerySchema querySchema_deal = QuerySchema.create().addSelect("*");
        QueryConditionGroup condition = new QueryConditionGroup();//银行对账单
        QueryConditionGroup condition_deal = new QueryConditionGroup();//银行交易明细
        if (StringUtils.isNotEmpty(accountid)) {
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("bankaccount").eq(accountid)));
            condition_deal.addCondition(QueryConditionGroup.and(QueryCondition.name("enterpriseBankAccount").eq(accountid)));
        }
        if (StringUtils.isNotEmpty(bankseqno)) {
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("bank_seq_no").eq(bankseqno)));
            condition_deal.addCondition(QueryConditionGroup.and(QueryCondition.name("bankseqno").eq(bankseqno)));
        }
        if (direction != null) {//借1  贷2
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("dc_flag").eq(direction)));
            condition_deal.addCondition(QueryConditionGroup.and(QueryCondition.name("dc_flag").eq(direction)));
        }
        if (tranTime != null) {
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("tran_time").eq(tranTime)));
            condition_deal.addCondition(QueryConditionGroup.and(QueryCondition.name("tranTime").eq(tranTime)));
        }
        if (tran_amt != null) {
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("tran_amt").eq(tran_amt)));
            condition_deal.addCondition(QueryConditionGroup.and(QueryCondition.name("tran_amt").eq(tran_amt)));
        }
        if (acctbal != null) {
            condition.addCondition(QueryConditionGroup.and(QueryCondition.name("acct_bal").eq(acctbal)));
            //生单的时候 余额调节表是这样存的
            condition_deal.addCondition(QueryConditionGroup.and(QueryCondition.name("acctbal").eq(acctbal.setScale(2, BigDecimal.ROUND_HALF_UP))));
        }
        querySchema.addCondition(condition);
        List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        if (bankReconciliationList == null || bankReconciliationList.size() == 0) {
            log.error("未匹配到银行对账单");
        } else if (bankReconciliationList.size() != 1) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102257"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18FB47DA04F8001C", "匹配到多张银行对账单，不允许删除，请检查") /* "匹配到多张银行对账单，不允许删除，请检查" */);
        } else {
            for (BankReconciliation bankReconciliation : bankReconciliationList) {
                //发布
                if (bankReconciliation.getIspublish()) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102258"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B7D0CA04700006", "该笔单据对应的交易流水[%s]已发生后续业务，不允许删除，请检查！") /* "该笔单据对应的交易流水[%s]已发生后续业务，不允许删除，请检查！" */, bankReconciliation.getBank_seq_no()));
                }
                //同步三方
                if (TripleSynchronStatus.AlreadyAuto.getValue() == bankReconciliation.getTripleSynchronStatus()
                        || TripleSynchronStatus.AlreadyManual.getValue() == bankReconciliation.getTripleSynchronStatus()) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102259"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B7D0CA04700006", "该笔单据对应的交易流水[%s]已发生后续业务，不允许删除，请检查！") /* "该笔单据对应的交易流水[%s]已发生后续业务，不允许删除，请检查！" */ , bankReconciliation.getBank_seq_no()));
                }
                //业务关联状态
                if (AssociationStatus.Associated.getValue() == bankReconciliation.getAssociationstatus()) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102259"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B7D0CA04700006", "该笔单据对应的交易流水[%s]已发生后续业务，不允许删除，请检查！") /* "该笔单据对应的交易流水[%s]已发生后续业务，不允许删除，请检查！" */ , bankReconciliation.getBank_seq_no()));
                }
                //已勾对流水不可再删除
                if ((ObjectUtils.isNotEmpty(bankReconciliation.getCheckflag()) && bankReconciliation.getCheckflag()) || (ObjectUtils.isNotEmpty(bankReconciliation.getOther_checkflag()) && bankReconciliation.getOther_checkflag())) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102597"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041802D7","该单据已勾对完成，不能删除单据！") /* "该单据已勾对完成，不能删除单据！" */);
                }
                bankReconciliation.setEntityStatus(EntityStatus.Delete);
            }
            querySchema_deal.addCondition(condition_deal);
            List<BankDealDetail> bankDealDetails = MetaDaoHelper.queryObject(BankDealDetail.ENTITY_NAME, querySchema_deal, null);
            if (CollectionUtils.isNotEmpty(bankDealDetails)) {
                if (bankDealDetails.size() != 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102260"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18FB47DA04F8001B","匹配到多张银行交易明細单，不允许删除，请检查") /* "匹配到多张银行交易明細单，不允许删除，请检查" */);
                }
                BankDealDetail bankDealDetail = bankDealDetails.get(0);
                YtsContext.setYtsContext("batchDeletebankDealDetails", bankDealDetail);
                bankDealDetail.setEntityStatus(EntityStatus.Delete);
                MetaDaoHelper.delete(BankDealDetail.ENTITY_NAME, bankDealDetail);
                //删除
                YtsContext.setYtsContext("batchDeleteBankReconciliation", bankReconciliationList);
                CommonBankReconciliationProcessor.batchReconciliationBeforeDelete(bankReconciliationList,ymsJdbcApi);
                MetaDaoHelper.delete(BankReconciliation.ENTITY_NAME, bankReconciliationList);
            }
        }
        QuerySchema querySchema_receipt = QuerySchema.create().addSelect("*");
        QueryConditionGroup condition_receipt = new QueryConditionGroup();
        condition_receipt.addCondition(QueryConditionGroup.and(QueryCondition.name(BankElectronicReceipt.BANKSEQNO).eq(bankseqno)));
        condition_receipt.addCondition(QueryConditionGroup.and(QueryCondition.name(BankElectronicReceipt.ENTERPRISE_BANK_ACCOUNT).eq(accountid)));
        querySchema_receipt.addCondition(condition_receipt);
        List<BankElectronicReceipt> bankElectronicReceiptList = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, querySchema_receipt, null);
        if (CollectionUtil.isNotEmpty(bankElectronicReceiptList) && bankElectronicReceiptList.size() == 1) {
            YtsContext.setYtsContext("batchDeleteBankReceipts", bankElectronicReceiptList);
            List<String> idList = bankElectronicReceiptList.stream().map(bizObject -> bizObject.getId().toString()).collect(Collectors.toList());
            bankReceiptService.deleteBankReceipts(idList);
        } else if (CollectionUtil.isNotEmpty(bankElectronicReceiptList) && bankElectronicReceiptList.size() > 1){
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_221E1B0E04380008", "匹配到多张回单，不允许删除，请检查") /* "匹配到多张回单，不允许删除，请检查" */);
        }

    }

    @Override
    public void cancelDeleteBankReconciliationByAccountDetail(BankReconciliationEntityDto param) throws Exception {
        //回滚数据
        BizObject oldbankDealDetail = (BizObject) YtsContext.getYtsContext("batchDeletebankDealDetails");
        if (oldbankDealDetail != null) {
            CmpMetaDaoHelper.insert(BankDealDetail.ENTITY_NAME, oldbankDealDetail);
        }
        ArrayList<BizObject> oldBankReconciliation = (ArrayList<BizObject>) YtsContext.getYtsContext("batchDeleteBankReconciliation");
        if (CollectionUtils.isNotEmpty(oldBankReconciliation)) {
            CmpMetaDaoHelper.insert(BankReconciliation.ENTITY_NAME, oldBankReconciliation);
        }
        ArrayList<BizObject> oldReceiptBizObjectList = (ArrayList<BizObject>) YtsContext.getYtsContext("batchDeleteBankReceipts");
        if (CollectionUtils.isNotEmpty(oldReceiptBizObjectList)) {
            CmpMetaDaoHelper.insert(BankElectronicReceipt.ENTITY_NAME, oldReceiptBizObjectList);
        }
        BankElectronicReceipt bankElectronicReceipt = (BankElectronicReceipt) oldReceiptBizObjectList.get(0);
        BankReconciliation bankReconciliation = (BankReconciliation) oldBankReconciliation.get(0);
        if (bankReconciliation.getId().toString().equals(bankElectronicReceipt.getBankreconciliationid())) {
            bankreconciliationService.sendEventOfFileidInFinal(bankReconciliation, bankElectronicReceipt.getExtendss());
        }
    }

    @Override
    public void deleteBankReconciliationByAccountDetailNoYTS(BankReconciliationEntityDto param) throws Exception {
        deleteBankReconciliationByAccountDetail(param);
    }

    @Override
    public void updateReconciliationIsImputation(List<BankReconciliationImputationVO> bankReconciliationImputationVOS) throws Exception {
        if (null == bankReconciliationImputationVOS || bankReconciliationImputationVOS.size() == 0){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102261"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800D2", "请求参数不能为空！") /* "请求参数不能为空！" */);
        }
        List<String> reconciliationIds = new ArrayList<>();
        Map<String,BankReconciliationImputationVO> bankReconciliationImputationVOMap = new HashMap<>();
        for(BankReconciliationImputationVO bankReconciliationImputationVO : bankReconciliationImputationVOS){
            reconciliationIds.add(bankReconciliationImputationVO.getId());
            bankReconciliationImputationVOMap.put(bankReconciliationImputationVO.getId(),bankReconciliationImputationVO);
        }
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("id").in(reconciliationIds));
        schema.addCondition(conditionGroup);
        List<BankReconciliation>  bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
        if (bankReconciliations == null){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102262"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800D1", "银行对账单不存在，请检查！") /* "银行对账单不存在，请检查！" */);
        }
        for(BankReconciliation bankReconciliation : bankReconciliations){
            BankReconciliationImputationVO bankReconciliationImputationVO = bankReconciliationImputationVOMap.get(bankReconciliation.getId().toString());
            //是否归集字段不为null  并且为是时更新该字段
            if(bankReconciliationImputationVO.getIsimputation() != null && bankReconciliationImputationVO.getIsimputation()){
                bankReconciliation.setIsimputation(bankReconciliationImputationVO.getIsimputation());
            }
            bankReconciliation.setIsinneraccounting(bankReconciliationImputationVO.getIsinnerAccounting());
            bankReconciliation.setImpinneraccount(bankReconciliationImputationVO.getInnerAccount());
            bankReconciliation.setEntityStatus(EntityStatus.Update);
        }
        CommonSaveUtils.updateBankReconciliation(bankReconciliations);
    }

    /**
     * 根据条件查询银行对账单信息
     * @param commonRequestDataVo
     * @return
     * @throws Exception
     */
    @Override
    public List<BankReconciliationVo> queryBankReconciliationList(CommonRequestDataVo commonRequestDataVo) throws Exception {
        log.error("批量查询银行交易明细-查询bip数据库参数params:{}", CtmJSONObject.toJSONString(commonRequestDataVo));
        List<String> bankReconciliationIds = new ArrayList<>();
        // key 银行对账单Id，value 认领单Id
        Map<Long, List<BillClaimItem>> idClaimMap;
        // 根据认领单Id查询银行对账单
        Map<Long, BillClaimItem> billClaimItemMap = findIdsByMyClaimIds(commonRequestDataVo.getClaimIds());
        if (billClaimItemMap != null) {
            for (Long key : billClaimItemMap.keySet()) {
                bankReconciliationIds.add(billClaimItemMap.get(key).getBankbill().toString());
            }
            idClaimMap = billClaimItemMap.values().stream().collect(Collectors.groupingBy(BillClaimItem::getBankbill));
        } else {
            idClaimMap = null;
        }

        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);

        //银行类别列表
        List<String> bankTypes = commonRequestDataVo.getBankTypeList();
        if (!CollectionUtils.isEmpty(bankTypes)) {
            conditionGroup.appendCondition(QueryCondition.name("banktype").in(bankTypes));
        }

        //银行账户列表
        List<String> enterpriseBankAccounts = commonRequestDataVo.getEnterpriseBankAccountList();
        if (!CollectionUtils.isEmpty(enterpriseBankAccounts)) {
            conditionGroup.appendCondition(QueryCondition.name("bankaccount").in(enterpriseBankAccounts));
        }

        //会计主体列表
        List<String> accentitys = commonRequestDataVo.getAccentityList();
        if (CollectionUtils.isEmpty(enterpriseBankAccounts) && !CollectionUtils.isEmpty(accentitys)) {
            conditionGroup.appendCondition(QueryCondition.name("accentity").in(accentitys));
        }

        //主键id
        String id = commonRequestDataVo.getId();
        if(StringUtils.isNotEmpty(id)){
            conditionGroup.appendCondition(QueryCondition.name("id").eq(id));
        }

        //主键id集合
        List<String> ids = commonRequestDataVo.getIds();

        // 认领单对应的银行对账单集合处理
        if (CollectionUtils.isNotEmpty(bankReconciliationIds)) {
            if (CollectionUtils.isEmpty(ids)) {
                ids = new ArrayList<>();
            }
            ids.addAll(bankReconciliationIds);
        }

        if(CollectionUtils.isNotEmpty(ids)){
            conditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        }

        // 开始日期
        String startDate = commonRequestDataVo.getStartDate();
        // 结束日期
        String endDate = commonRequestDataVo.getEndDate();
        if(StringUtils.isNotEmpty(startDate) && StringUtils.isNotEmpty(endDate)){
            conditionGroup.appendCondition(QueryCondition.name("tran_date").between(startDate, endDate));
        }

        if (commonRequestDataVo.getInitFlag() != null) {
            // 与列表查询保持一致，过滤掉期初未达项设置的期初数据
            if (!commonRequestDataVo.getInitFlag().equals(1) && !commonRequestDataVo.getInitFlag().equals(0)) {
                log.error("传入的期初未达项错误{}", commonRequestDataVo.getInitFlag());
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400593", "传入的期初未达项错误！") /* "传入的期初未达项错误！" */);
            }
            conditionGroup.appendCondition(QueryCondition.name("initflag").eq(commonRequestDataVo.getInitFlag()));
        }
        schema.appendQueryCondition(conditionGroup);
        List<BankReconciliation> bankReconciliations = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, schema, null);
        if(CollectionUtils.isEmpty(bankReconciliations)){
            log.error("查询的银行对账单为空");
            return new ArrayList<>();
        }
        log.error("批量查询银行交易明细-查询bip数据库查询数据量为:{}", bankReconciliations.size());
        List<String> bankRecIdList = new ArrayList<>();
        List<BankReconciliationVo> bankReconciliationVos = new ArrayList<>();
        // 处理认领单相关信息
        handleClaimInfo(bankReconciliations, bankReconciliationVos,bankRecIdList, idClaimMap);
        QuerySchema subSchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup subConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        subConditionGroup.appendCondition(QueryCondition.name("bankreconciliation").in(bankRecIdList));
        subSchema.appendQueryCondition(subConditionGroup);
        List<BankReconciliationbusrelation_b> bankReconRels = MetaDaoHelper.queryObject(BankReconciliationbusrelation_b.ENTITY_NAME, subSchema, null);
        List<BankReconciliationbusrelationVo> bankRecRelVos = new ArrayList<>();

        for (BankReconciliationbusrelation_b  e : bankReconRels) {
            bankRecRelVos.add(bankreconciliationService.convertBankRecRel2BankRelVO(e));
        }
        Map<String, List<BankReconciliationbusrelationVo>> groupedBankRecRelVo = bankRecRelVos.stream().collect(Collectors.groupingBy(BankReconciliationbusrelationVo::getBankreconciliation));
        bankReconciliationVos.forEach(e -> {
            List<BankReconciliationbusrelationVo> relList = groupedBankRecRelVo.get(e.getId());
            e.setBankReconciliationbusrelationVos(relList);
        });
        return bankReconciliationVos;
    }

    /**
     * 处理认领单信息
     * @param bankReconciliations 银行对账单集合
     * @param bankRecIdList 银行对账单id集合
     * @param bankReconciliationVos 银行对账单vo集合
     * @param idClaimMap key 为银行对账单id，value 为认领单集合
     */
    private void handleClaimInfo(List<BankReconciliation> bankReconciliations, List<BankReconciliationVo> bankReconciliationVos, List<String> bankRecIdList,
                                 Map<Long, List<BillClaimItem>> idClaimMap) {
        bankReconciliations.forEach(e -> {
            BankReconciliationVo vo = bankreconciliationService.convertBankReconciliation2BankReconciliationVO(e);
            if (idClaimMap != null) {
                List<BillClaimItem> claimItemList = idClaimMap.get(Long.parseLong(vo.getId()));
                // 补充认领单Id
                if (CollectionUtils.isNotEmpty(claimItemList)) {
                    List<String> claimItems = new ArrayList<>();
                    for (BillClaimItem item : claimItemList) {
                        claimItems.add(item.getId().toString());
                    }
                    vo.setClaimIdList(claimItems);
                }
            }
            bankReconciliationVos.add(vo);
            bankRecIdList.add(e.getId().toString());
        });
    }

    /**
     * 根据认领单Id查询银行对账单Id
     * @param claimIds 认领单Id 为空直接返回
     * @return 银行对账单Id集合
     */
    private Map<Long, BillClaimItem> findIdsByMyClaimIds(List<Long> claimIds) throws Exception {
        if (CollectionUtils.isEmpty(claimIds)) {
            return null;
        }

        QuerySchema querySchema = QuerySchema.create().addSelect("id,tran_date, bankbill,mainid");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name(ICmpConstant.MAINID).in(claimIds));
        querySchema.appendQueryCondition(conditionGroup);
        List<BillClaimItem> billClaimItems = MetaDaoHelper.queryObject(BillClaimItem.ENTITY_NAME, querySchema, null);
        if (CollectionUtils.isEmpty(billClaimItems)) {
            return null;
        }

        return billClaimItems.stream().collect(Collectors.toMap(BillClaimItem::getMainid,
                item -> item, (v1, v2) -> v1.getTran_date().getTime() > v2.getTran_date().getTime() ? v1 : v2));
    }

}