package com.yonyoucloud.fi.cmp.internaltransferprotocol.rule.business;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.basedoc.model.BankAcctCurrencyVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.AuthUtil;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.cmpentity.IsEnable;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.event.utils.DetermineUtils;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.CmpInternalTransferProtocolCharacterDef;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.CmpTransfereeInformationCharacterDef;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.InternalTransferProtocol;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.TransfereeInformation;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.enums.ApportionmentMethod;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.enums.DataSources;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.enums.TransferOutAccountAllocation;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.business.BillImportCheckUtil;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.biz.base.BizException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <h1>内转协议保存前置规则</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023-09-11 9:55
 */
@Component("beforeSaveProtocolBillRule")
@RequiredArgsConstructor
public class BeforeSaveProtocolBillRule extends AbstractCommonRule {
    // 该集合用于校验表头字段是否编辑调整过
    private static final List<String> IS_EDIT_BILL_FIELD_LIST = Arrays.asList(ICmpConstant.ACCENTITY, ICmpConstant.INTERNAL_TRANSFER_PROTOCOL_CODE,
            ICmpConstant.PROJECT, ICmpConstant.CONTRACT_NO, ICmpConstant.CONTRACT_NAME, ICmpConstant.IS_ENABLED_TYPE, ICmpConstant.ORG, ICmpConstant.DESCRIPTION,
            ICmpConstant.TRANSFER_OUT_ACCOUNT_ALLOCATION, ICmpConstant.ENTERPRISE_BANK_ACCOUNT_LOWER, ICmpConstant.ACCT_OPEN_TYPE, ICmpConstant.CURRENCY);

    private final BaseRefRpcService baseRefRpcService;
    private final YmsOidGenerator ymsOidGenerator;
    private final EnterpriseBankQueryService enterpriseBankQueryService;


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        // 校验入参
        validateBills(bills);
        // 获取要处理的单据
        BizObject bizObject = bills.get(0);
        // 逻辑处理
        executeForBizObject(bizObject);
        // 检查内转协议是否需要更新版本，如果需要则升级版本
        updateVersionIfNeeded(bizObject);
        // 返回
        return new RuleExecuteResult();
    }

    private void validateBills(List<BizObject> bills) {
        if (bills.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101920"),getMessage("UID:P_CM-BE_1932EBC204080086", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC204080086", "传入的单据为空，请检查！") /* "传入的单据为空，请检查！" */) /* "传入的单据为空，请检查！" */);
        }
        List<TransfereeInformation> bList = bills.get(0).getBizObjects(ICmpConstant.TRANSFEREE_INFORMATION,
                TransfereeInformation.class);
        if (CollectionUtils.isNotEmpty(bList)) {
            for (TransfereeInformation item : bList) {
                //CM202400472传结算的现金管理单据控制0金额数据不传给结算
                if (!Objects.isNull(item.getFixedAmount()) && item.getFixedAmount().compareTo(BigDecimal.ZERO) == 0) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E86313A05200002", "金额为0的单据无法保存！") /* "金额为0的单据无法保存！" */);
                }
                if (!Objects.isNull(item.getApportionmentRatio()) && item.getApportionmentRatio().compareTo(BigDecimal.ZERO) == 0) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1E86444405200003", "分摊比例不能为0！") /* "分摊比例不能为0！" */);
                }
            }
        }
    }

    private void executeForBizObject(BizObject bizObject) throws Exception {
        String status = bizObject.getEntityStatus().name();
        // 处理导入的数据
        dataCheckAndProcessing(bizObject);
        // 获取明细数据
        List<TransfereeInformation> transfereeInformationList = prepareTransfereeInformationList(status, bizObject);
        // 校验银行账户数据
        if (CollectionUtils.isNotEmpty(transfereeInformationList)) {
            checkEnterpriseBankAccount(bizObject, transfereeInformationList);
        }
    }

    private void dataCheckAndProcessing(BizObject bizObject) throws Exception {
        if (bizObject.containsKey(ICmpConstant.FROM_API) || Boolean.TRUE.equals(bizObject.getBoolean(ICmpConstant.FROM_API))) {
            if(EntityStatus.Update.equals(bizObject.getEntityStatus())){
                BizObject bizObjectDb = MetaDaoHelper.findById(InternalTransferProtocol.ENTITY_NAME, bizObject.getId(), 1);
                if (bizObjectDb.getShort(ICmpConstant.DATA_SOURCES) != DataSources.THIRD_PARTY.getValue()){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101921"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19CC055C04C00026","单据不是通过OpenAPI接口新增的，不允许修改！" ) + bizObjectDb.getString("code")/* "单据不是通过OpenAPI接口新增的，不允许修改！" */);
                }
                bizObject.set("code", bizObjectDb.getString("code"));
            }
            // 校验项目
            checkProject(bizObject);
            // 校验账号
            checkMainEnterpriseBankAccount(bizObject);
            importDataProcessHandler(bizObject);
        }
    }

    private void checkMainEnterpriseBankAccount(BizObject bizObject) throws Exception {
        String enterpriseBankAccount = bizObject.getString(ICmpConstant.ENTERPRISE_BANK_ACCOUNT_LOWER);
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setId(enterpriseBankAccount);
        enterpriseParams.setOrgid(bizObject.getString(ICmpConstant.ACCENTITY));
        enterpriseParams.setCurrencyIDList(Collections.singletonList(bizObject.getString(ICmpConstant.CURRENCY)));
        List<EnterpriseBankAcctVO> bankAccounts = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
        if (bankAccounts.isEmpty()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101922"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19CC055C04C00021","根据币种、会计主体与银行账户查询企业银行账户表没有数据！")+ bizObject.getString("code") /* "根据币种、会计主体与银行账户查询企业银行账户表没有数据！" */);
        }
    }

    private void checkProject(BizObject bizObject) throws Exception {
        if (bizObject.get(ICmpConstant.PROJECT) != null) {
            List<Map<String, Object>> projectList = QueryBaseDocUtils.queryProjectById(bizObject.get(ICmpConstant.PROJECT));
            if (!CollectionUtils.isEmpty(projectList)) {
                Object projectFlag = projectList.get(0).get("enable");
                if (projectFlag != null && "2".equals(projectFlag.toString())) {//1是启用，2是未启用
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101923"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180810","项目未启用，保存失败！")+ bizObject.getString("code") /* "项目未启用，保存失败！" */);
                }
                Object orgId = projectList.get(0).get(ICmpConstant.ORG_ID);
                if ("666666".equals(orgId)){
                    return;
                }
                List<String> project = new ArrayList<>();
                project.add(bizObject.get(ICmpConstant.PROJECT));
                List<String> orgIds = BillImportCheckUtil.queryOrgRangeSByProjectIds(project);
                // 核算委托关系的没有校验，后续优化，该逻辑后续调整为批量处理，要优化
                assert orgIds != null;
                String accent = bizObject.get(IBussinessConstant.ACCENTITY).toString();
                if (!orgIds.contains(accent)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101924"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180814","导入的项目所属使用组织与导入会计主体不一致！")+ bizObject.getString("code") /* "导入的项目所属使用组织与导入会计主体不一致！" */);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101925"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180819","未查询到对应的项目，保存失败！")+ bizObject.getString("code") /* "未查询到对应的项目，保存失败！" */);
            }
        }
    }


    private List<TransfereeInformation> prepareTransfereeInformationList(String status, BizObject bizObject) throws Exception {
        // 新增时校验主表：会计主体+项目+币种 唯一性
        checkLockAndQuerySchema(bizObject, status);

        // 计算处理明细数据
        return getTransfereeInformationList(bizObject);
    }

    private List<TransfereeInformation> getTransfereeInformationList(BizObject bizObject) throws Exception {
        // 编辑修改时，取数据库和前端页面的明细数据
        List<TransfereeInformation> transfereeInformationListPageOriginal = bizObject.getBizObjects(ICmpConstant.TRANSFEREE_INFORMATION, TransfereeInformation.class);
        if(CollectionUtils.isEmpty(transfereeInformationListPageOriginal)){
            return Collections.emptyList();
        }
        List<TransfereeInformation> transfereeInformationListPage = new ArrayList<>();
        CollectionUtils.addAll(transfereeInformationListPage, new TransfereeInformation[transfereeInformationListPageOriginal.size()]);
        Collections.copy(transfereeInformationListPage, transfereeInformationListPageOriginal);
        BizObject bizObjectDb = MetaDaoHelper.findById(InternalTransferProtocol.ENTITY_NAME, bizObject.getId(), 2);
        List<TransfereeInformation> transfereeInformationListDb = null;
        if (!ValueUtils.isEmpty(bizObjectDb)){
            transfereeInformationListDb = bizObjectDb.getBizObjects(ICmpConstant.TRANSFEREE_INFORMATION, TransfereeInformation.class);
        }
        List<TransfereeInformation> transfereeInformationList = mergeTransfereeInformationLists(transfereeInformationListPage, transfereeInformationListDb);
        // 校验子表明细：内转协议明细相同转入会计主体、相同费用项目、相同银行账号需要增加校验，判断唯一性
        verifyDetailData(transfereeInformationList, bizObject);
        return transfereeInformationList;
    }

    private void verifyDetailData(List<TransfereeInformation> transfereeInformationList,BizObject bizObject) throws Exception {
        Set<String> uniqueValidation = new HashSet<>();
        Set<String> uniqueValidationMainAndSubAccount = new HashSet<>();
        Set<Short> uniqueValidationMainAndSubAcctOpenType = new HashSet<>();
        for (TransfereeInformation transfereeInformation : transfereeInformationList) {
            String value = transfereeInformation.getIntoAccentity().concat("_").concat(transfereeInformation.getExpenseitem().toString());
            if(ValueUtils.isNotEmptyObj(transfereeInformation.getEnterpriseBankAccount())){
                value = value.concat("_").concat(transfereeInformation.getEnterpriseBankAccount());
            }
            uniqueValidation.add(value);
            if (ValueUtils.isNotEmptyObj(transfereeInformation.getEnterpriseBankAccount())){
                uniqueValidationMainAndSubAccount.add(transfereeInformation.getEnterpriseBankAccount());
            }

            if(ValueUtils.isNotEmptyObj(transfereeInformation.getEnterpriseBankAccount())){
                EnterpriseParams enterpriseParams = new EnterpriseParams();
                enterpriseParams.setId(transfereeInformation.getEnterpriseBankAccount());
                List<EnterpriseBankAcctVO> bankAccounts = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
                if (!bankAccounts.isEmpty()){
                    EnterpriseBankAcctVO enterpriseBankAcctVO = bankAccounts.get(0);
                    uniqueValidationMainAndSubAcctOpenType.add(Short.parseShort(enterpriseBankAcctVO.getAcctopentype().toString()));
                }
            }
        }
        if (uniqueValidation.size()!= transfereeInformationList.size()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101926"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19CC055C04C00023","子表明细行校验失败，明细行上相同的转入会计主体、相同的费用项目、相同的银行账号需唯一！")+ bizObject.getString("code") /* "子表明细行校验失败，明细行上相同的转入会计主体、相同的费用项目、相同的银行账号需唯一！" */);
        }
        if (ValueUtils.isNotEmptyObj(bizObject.getString(ICmpConstant.ENTERPRISE_BANK_ACCOUNT_LOWER))
                && uniqueValidationMainAndSubAccount.contains(bizObject.getString(ICmpConstant.ENTERPRISE_BANK_ACCOUNT_LOWER))){
            //2024-04-08 适配河北建工场景 如果YMS不包含当前参数 或者 有参数 标识不允许相同
            String isSameAccount = AppContext.getEnvConfig(ICmpConstant.INTERNALTRANS_YMS_SAMEACCOUNT);
            if (StringUtils.isEmpty(isSameAccount) || !Boolean.parseBoolean(isSameAccount)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101927"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19CC055C04C00024", "明细行上的银行账号不能与表头上的银行账号相等！")+ bizObject.getString("code") /* "明细行上的银行账号不能与表头上的银行账号相等！" */);
            }
        }

        if (bizObject.getShort(ICmpConstant.IS_ENABLED_TYPE) == IsEnable.DISENABLE.getValue()){
            return;
        }
        if (TransferOutAccountAllocation.SETUP_ACCOUNT_MANUALLY.getValue()== bizObject.getShort(ICmpConstant.TRANSFER_OUT_ACCOUNT_ALLOCATION)
                && (uniqueValidationMainAndSubAcctOpenType.size()==1
                && !uniqueValidationMainAndSubAcctOpenType.contains(bizObject.getShort(ICmpConstant.ACCT_OPEN_TYPE)))){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101928"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19CC055C04C00025","明细行上的开户类型要与表头上的开户类型相等！") /* "明细行上的开户类型要与表头上的开户类型相等！" */);
        }
    }

    private List<TransfereeInformation> mergeTransfereeInformationLists(List<TransfereeInformation> pageList, List<TransfereeInformation> dbList) {
        // 如果页面的明细数据为空，则返回空
        if (CollectionUtils.isEmpty(pageList)) {
            return Collections.emptyList();
        }
        // 否则将页面和数据库明细数据转换为Map集合
        pageList.forEach(item -> {if (!ValueUtils.isNotEmptyObj(item.getId())){item.setId(ymsOidGenerator.nextId());}});
        Map<Object, TransfereeInformation> mapPage = pageList.stream().filter(item -> !item.getEntityStatus().name().equals(EntityStatus.Delete.name()))
                .collect(Collectors.toMap(TransfereeInformation::getId, item -> item));
        // 如果更新时只是删除明细数据也不需要处理，直接返回空
        if (ValueUtils.isEmpty(mapPage)){
            return Collections.emptyList();
        }
        List<Long> filterPageIdsList = pageList.stream().filter(item -> item.getEntityStatus().name().equals(EntityStatus.Update.name())
                || item.getEntityStatus().name().equals(EntityStatus.Delete.name())).map(item -> Long.parseLong(item.getId().toString())).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(dbList)){
            Map<Object, TransfereeInformation> mapDb = dbList.stream().collect(Collectors.toMap(TransfereeInformation::getId, item -> item));
            mergeMaps(mapDb, filterPageIdsList, pageList);
        }
        return pageList.stream().filter(item -> !ValueUtils.isNotEmptyObj(item.getEntityStatus())
                || !item.getEntityStatus().name().equals(EntityStatus.Delete.name())  ).collect(Collectors.toList());
    }

    private void mergeMaps(Map<Object, TransfereeInformation> mapDb, List<Long> filterPageIdsList, List<TransfereeInformation> pageList) {
        // 合并页面明细数据和数据库明细数据
        for (Map.Entry<Object, TransfereeInformation> entry : mapDb.entrySet()) {
            Long key = Long.parseLong(entry.getKey().toString());
            if (!filterPageIdsList.contains(key)) {
                pageList.add(entry.getValue());
            }
        }
    }

    private void checkEnterpriseBankAccount(BizObject bizObject, List<TransfereeInformation> transfereeInformationList) throws Exception {
        for (TransfereeInformation transfereeInformation : transfereeInformationList) {
            if (ValueUtils.isNotEmptyObj(transfereeInformation.getEnterpriseBankAccount())) {
                validateEnterpriseParamsSub(bizObject, transfereeInformation, transfereeInformation.getEnterpriseBankAccount());
            }
        }
        checkEnterpriseBankAccountOnMatch(bizObject, transfereeInformationList);
    }

    private void checkEnterpriseBankAccountOnMatch(BizObject bizObject, List<TransfereeInformation> transfereeInformationList) {
        boolean match = transfereeInformationList.stream().anyMatch(item -> !ValueUtils.isNotEmptyObj(item.getEnterpriseBankAccount()));
        if (match && bizObject.getShort(ICmpConstant.IS_ENABLED_TYPE) == IsEnable.ENABLE.getValue()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101929"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_197C80000498000F", "主表启停用状态为启用时，则明细行的银行账号必填！")+ bizObject.getString("code"));
        }
    }

    private void checkLockAndQuerySchema(BizObject bizObject, String status) throws Exception {
        String lockKey = composeLockKey(bizObject);
        YmsLock ymsLock = null;
        try {
            ymsLock = JedisLockUtils.lockBillWithOutTrace(lockKey);
            // 加锁
            if (null == ymsLock) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101930"),MessageUtils.getMessage("P_YS_FI_CM_0001289877") /* "该数据正在处理，请稍后重试！" */);
            }
            // 根据会计主体+项目+币种+父id为null+未废弃，查询数据
            QuerySchema schema = composeQuerySchema(bizObject);
            List<Map<String, Object>> list = MetaDaoHelper.query(InternalTransferProtocol.ENTITY_NAME, schema);

            if ("Update".equals(status)){
                List<Long> ids = list.stream().map(item -> Long.parseLong(item.get("id").toString())).collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(ids) && ids.contains(Long.parseLong(bizObject.getId().toString()))){
                    return;
                }
            }

            // 若数据已存在则抛出异常
            validateList(list, bizObject);
        } catch (Exception e) {
            throw e;
        } finally {
            // 释放锁
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
    }

    private QuerySchema composeQuerySchema(BizObject bizObject) {
        QueryConditionGroup queryConditionGroup = QueryConditionGroup.and(
                QueryCondition.name(ICmpConstant.ACCENTITY).eq(bizObject.get(ICmpConstant.ACCENTITY)),
                QueryCondition.name(ICmpConstant.PROJECT).eq(bizObject.get(ICmpConstant.PROJECT)),
                QueryCondition.name(ICmpConstant.CURRENCY).eq(bizObject.get(ICmpConstant.CURRENCY)),
                QueryCondition.name(ICmpConstant.PARENT_ID).is_null(),
                QueryCondition.name(ICmpConstant.IS_DISCARD).eq((short) 0)
        );
        if (ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.CONTRACT_NO))){
            queryConditionGroup.appendCondition(QueryCondition.name(ICmpConstant.CONTRACT_NO).eq(bizObject.get(ICmpConstant.CONTRACT_NO)));
        }
        QuerySchema schema = QuerySchema.create().addSelect("id");
        schema.appendQueryCondition(queryConditionGroup);
        return schema;
    }

    private String composeLockKey(BizObject bizObject) {
        // 组装Redis锁的key值
        return IBillNumConstant.CMP_INTERNALTRANSFERPROTOCOL
                + bizObject.get(ICmpConstant.ACCENTITY).toString().concat(ICmpConstant.UNDER_LINE)
                .concat(bizObject.get(ICmpConstant.PROJECT).toString())
                .concat(ICmpConstant.UNDER_LINE)
                .concat(bizObject.get(ICmpConstant.CURRENCY).toString())
                .concat(ICmpConstant.UNDER_LINE)
                .concat(ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.CONTRACT_NO))
                                ? bizObject.get(ICmpConstant.CONTRACT_NO).toString() : ICmpConstant.CONTRACT_NO);
    }

    private void validateList(List<Map<String, Object>> list, BizObject bizObject) {
        if (CollectionUtils.isNotEmpty(list) && list.size() >= ICmpConstant.CONSTANT_ONE) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005A3", "当前资金组织下，相同项目、合同号与币种已存在数据，请勿重复维护!") /* "当前资金组织下，相同项目、合同号与币种已存在数据，请勿重复维护!" */);
        }
    }

    private void updateVersionIfNeeded(BizObject bizObject) throws Exception {
        if (EntityStatus.Update.name().equals(bizObject.getEntityStatus().name())) {
            InternalTransferProtocol currentBill = MetaDaoHelper.findById(InternalTransferProtocol.ENTITY_NAME, bizObject.getId(), 2);
            boolean isEditBill = isBillEdited(bizObject, currentBill);
            List<BizObject> protocolCallLogsList = currentBill.get(ICmpConstant.PROTOCOL_CALL_LOGS);
            updateVersionAndVersionNo(bizObject, currentBill, isEditBill, protocolCallLogsList);
        }
    }

    private boolean isBillEdited(BizObject bizObject, InternalTransferProtocol currentBill) {
        boolean isEditBill = false;
        if (currentBill.getVersionId() > 0) {
            List<BizObject> transfereeInformationHistoryList = bizObject.get(ICmpConstant.TRANSFEREE_INFORMATION);
            if (CollectionUtils.isEmpty(transfereeInformationHistoryList)) {
                // 编辑保存时，子表为空，则与数据库比较主表各个字段，是否发生变化
                isEditBill = compareKeys(bizObject, currentBill);
            } else {
                // 编辑保存时，子表不为空，则一定调整了数据，升级版本
                isEditBill = true;
            }
        }
        return isEditBill;
    }

    private boolean compareKeys(BizObject bizObject, InternalTransferProtocol currentBill) {
        boolean isEditBill = false;
        for (String key : IS_EDIT_BILL_FIELD_LIST) {
            Object pageData = bizObject.get(key);
            Object dbData = currentBill.get(key);
            // 比较主表各个字段
            if (ValueUtils.isNotEmptyObj(pageData) && ValueUtils.isNotEmptyObj(dbData)) {
                isEditBill = Objects.equals(pageData.toString(), dbData.toString());
            }
            if (differentCasesInBill(pageData, dbData)) {
                isEditBill = true;
            }
        }
        return isEditBill;
    }

    private boolean differentCasesInBill(Object pageData, Object dbData) {
        return ValueUtils.isNotEmptyObj(pageData) && !ValueUtils.isNotEmptyObj(dbData) ||
                !ValueUtils.isNotEmptyObj(pageData) && ValueUtils.isNotEmptyObj(dbData);
    }

    private void updateVersionAndVersionNo(BizObject bizObject, InternalTransferProtocol currentBill, boolean isEditBill, List<BizObject> protocolCallLogsList) throws Exception {
        if (isEditBill && CollectionUtils.isNotEmpty(protocolCallLogsList)) {
            // 升级版本与版本号
            String versionNo = currentBill.getVersionNo();
            long size = protocolCallLogsList.stream()
                    .filter(item -> versionNo.equals(item.getString(ICmpConstant.CALLER_PROTOCOL_VERSION)))
                    .count();
            if (size >= 1L) {
                Long versionId = currentBill.getLong(ICmpConstant.VERSION_ID);
                versionId = versionId + 1L;
                bizObject.set(ICmpConstant.VERSION_ID, versionId);
                bizObject.set(ICmpConstant.VERSION_NO, ICmpConstant.CONSTANT_V.concat(versionId.toString()));
                maintainHistoricalVersionInformation(bizObject.getId());
            }
        }
    }

    private void maintainHistoricalVersionInformation(Object mainId) throws Exception {
        long id = ymsOidGenerator.nextId();
        BizObject internalTransferProtocolHistory = MetaDaoHelper.findById(InternalTransferProtocol.ENTITY_NAME, mainId, 2);
        internalTransferProtocolHistory.set(ICmpConstant.PARENT_ID, mainId.toString());
        internalTransferProtocolHistory.set(ICmpConstant.PRIMARY_ID, id);
        internalTransferProtocolHistory.set(ICmpConstant.IS_PARENT, ICmpConstant.CONSTANT_ZERO);
        internalTransferProtocolHistory.set(ICmpConstant.PROTOCOL_CALL_LOGS, null);
        internalTransferProtocolHistory.set(ICmpConstant.CREATE_DATE, new Date());
        internalTransferProtocolHistory.set(ICmpConstant.CREATE_TIME, new Date());
        internalTransferProtocolHistory.set(ICmpConstant.CREATOR, InvocationInfoProxy.getUsername());
        internalTransferProtocolHistory.set(ICmpConstant.MODIFIER, null);
        internalTransferProtocolHistory.set(ICmpConstant.MODIFY_TIME, null);
        internalTransferProtocolHistory.set(ICmpConstant.MODIFY_DATE, null);
        CmpInternalTransferProtocolCharacterDef internalTransferProtocolCharacterDef
                = internalTransferProtocolHistory.get("internalTransferProtocolCharacterDef");
        if (ValueUtils.isNotEmptyObj(internalTransferProtocolCharacterDef)) {
            internalTransferProtocolCharacterDef.put("id", id);
            internalTransferProtocolCharacterDef.put("_status", EntityStatus.Insert.name());
        }
        // 设置单据编码
        internalTransferProtocolHistory.put(ICmpConstant.CODE, internalTransferProtocolHistory.get("code")
                + "_"+ internalTransferProtocolHistory.getString("versionNo"));
        List<BizObject> transfereeInformationHistoryList = internalTransferProtocolHistory.get(ICmpConstant.TRANSFEREE_INFORMATION);
        for (BizObject transfereeInformationHistory : transfereeInformationHistoryList) {
            long subId = ymsOidGenerator.nextId();
            transfereeInformationHistory.set(ICmpConstant.PRIMARY_ID, subId);
            transfereeInformationHistory.set(ICmpConstant.MAINID, id);
            transfereeInformationHistory.put(ICmpConstant.ENTITY_STATUS, EntityStatus.Insert);
            CmpTransfereeInformationCharacterDef transfereeInformationCharacterDef
                    = transfereeInformationHistory.get("transfereeInformationCharacterDef");
            if (ValueUtils.isNotEmptyObj(transfereeInformationCharacterDef)) {
                transfereeInformationCharacterDef.put("_status", EntityStatus.Insert.name());
                transfereeInformationCharacterDef.put("id", subId);
            }
        }
        internalTransferProtocolHistory.setEntityStatus(EntityStatus.Insert);
        CmpMetaDaoHelper.insert(InternalTransferProtocol.ENTITY_NAME, internalTransferProtocolHistory);
    }

    private void importDataProcessHandler(BizObject bizObject) throws Exception {
        // 来源OpenAPI的数据，校验主表数据
        validateMainData(bizObject);
        List<TransfereeInformation> transfereeInformationList = bizObject.getBizObjects("TransfereeInformation", TransfereeInformation.class);
        for (TransfereeInformation transfereeInformation : transfereeInformationList) {
            // 来源OpenAPI的数据，校验子表数据
            validateSubData(transfereeInformation);
        }
        // 校验分摊方式为按比例分摊时，所有明细行分摊比例之和不能大于100%
        validateApportionmentRatioSum(transfereeInformationList);
        // 设置单据编码
        CmpCommonUtil.billCodeHandler(bizObject, InternalTransferProtocol.ENTITY_NAME, ICmpConstant.CMP_POINT_INTERNALTRANSFERPROTOCOL);
        // 设置常量数据
        setConstantData(bizObject);
    }

    private void validateMainData(BizObject bizObject) throws Exception {
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.ACCENTITY))).throwMessage(getMessage("UID:P_CM-BE_1942999405580057", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1942999405580057", "主表的会计主体不能为空！") /* "主表的会计主体不能为空！" */) /* "主表的会计主体不能为空！" */);
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.PROJECT))).throwMessage(getMessage("UID:P_CM-BE_1942999405580058", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1942999405580058", "主表的项目不能为空！") /* "主表的项目不能为空！" */) /* "主表的项目不能为空！" */);
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.IS_ENABLED_TYPE))).throwMessage(getMessage("UID:P_CM-BE_194299940558005A", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_194299940558005A", "主表的启停用状态不能为空！") /* "主表的启停用状态不能为空！" */) /* "主表的启停用状态不能为空！" */);
        Short transferOutAccountAllocation = bizObject.getShort(ICmpConstant.TRANSFER_OUT_ACCOUNT_ALLOCATION);
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.CURRENCY))).throwMessage(getMessage("UID:P_CM-BE_194299940558005D", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_194299940558005D", "主表的币种不能为空！") /* "主表的币种不能为空！" */) /* "主表的币种不能为空！" */);
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(transferOutAccountAllocation)).throwMessage(getMessage("UID:P_CM-BE_194299940558005C", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_194299940558005C", "主表的转出账户分配不能为空！") /* "主表的转出账户分配不能为空！" */) /* "主表的转出账户分配不能为空！" */);
        if (transferOutAccountAllocation == TransferOutAccountAllocation.SETUP_ACCOUNT_MANUALLY.getValue()) {
            DetermineUtils.isTure(ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.ENTERPRISE_BANK_ACCOUNT_LOWER))).throwMessage(getMessage("UID:P_CM-BE_194299940558005F", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_194299940558005F", "主表的银行账户不能为空！") /* "主表的银行账户不能为空！" */) /* "主表的银行账户不能为空！" */);
            Object currency = bizObject.get(ICmpConstant.CURRENCY);
            String enterpriseBankAccount = bizObject.getString(ICmpConstant.ENTERPRISE_BANK_ACCOUNT_LOWER);
            EnterpriseParams enterpriseParams = new EnterpriseParams();
            enterpriseParams.setId(enterpriseBankAccount);
            enterpriseParams.setOrgid(bizObject.getString(ICmpConstant.ACCENTITY));
            enterpriseParams.setCurrencyIDList(Collections.singletonList(currency.toString()));
            List<EnterpriseBankAcctVO> bankAccounts = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
            if (!bankAccounts.isEmpty()) {
                EnterpriseBankAcctVO enterpriseBankAcctVO = bankAccounts.get(0);
                String orgId = enterpriseBankAcctVO.getOrgid();
                if (!bizObject.getString(ICmpConstant.ACCENTITY).equals(orgId)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101932"),getMessage("UID:P_CM-BE_19435B1804F00023", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19435B1804F00023", "当前主表导入的银行账户不在会计主体下！") /* "当前主表导入的银行账户不在会计主体下！" */) /* "当前主表导入的银行账户不在会计主体下！" */);
                }
                Integer acctOpenType = enterpriseBankAcctVO.getAcctopentype();
                if (!ValueUtils.isNotEmptyObj(acctOpenType)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101257"),getMessage("UID:P_CM-BE_1932EBC20408007E", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_194D491804F00013", "表头上填写的银行账户对应的开户类型为空！") /* "表头上填写的银行账户对应的开户类型为空！" */) /* "表头上填写的银行账户对应的开户类型为空！" */);
                }
                bizObject.set(ICmpConstant.ACCT_OPEN_TYPE, acctOpenType);
                List<BankAcctCurrencyVO> currencyList = enterpriseBankAcctVO.getCurrencyList();
                if (currencyList.isEmpty()) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101933"),getMessage("UID:P_CM-BE_19435B1804F00021", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19435B1804F00021", "当前导入的银行账户没有币种") /* "当前导入的银行账户没有币种" */) /* "当前导入的银行账户没有币种" */);
                }
            }
        } else {
            bizObject.set(ICmpConstant.ENTERPRISE_BANK_ACCOUNT_LOWER, null);
        }
    }


    private void validateSubData(TransfereeInformation transfereeInformation) throws Exception {
        ensureSubDataValidity(transfereeInformation);
        List<Map<String, Object>> expenseItemList = QueryBaseDocUtils.queryExpenseItemById(transfereeInformation.getExpenseitem());/* 暂不修改 已登记*/
        if (!CollectionUtils.isEmpty(expenseItemList)) {
            validateExpenseItemList(transfereeInformation, expenseItemList);
        }
        ensureSubDataApportionmentMethod(transfereeInformation);
    }

    private void ensureSubDataValidity(TransfereeInformation transfereeInformation) throws CtmException {
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(transfereeInformation.getIntoAccentity())).throwMessage(getMessage("UID:P_CM-BE_1942999405580054", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1942999405580054", "子表的转入会计主体不能为空！") /* "子表的转入会计主体不能为空！" */));
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(transfereeInformation.getExpenseitem())).throwMessage(getMessage("UID:P_CM-BE_1942999405580055", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1942999405580055", "子表的费用项目不能为空！") /* "子表的费用项目不能为空！" */));
    }

    private void validateExpenseItemList(TransfereeInformation transfereeInformation, List<Map<String, Object>> expenseItemList) throws Exception {
        Object expenseItemFlag = expenseItemList.get(0).get(ICmpConstant.ENABLED);
        if (expenseItemFlag != null && !(boolean) expenseItemFlag) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101934"),getMessage("UID:P_CM-BE_17FE8C540418002C", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_194D491804F00011", "费用项目未启用，保存失败！") /* "费用项目未启用，保存失败！" */));
        }
        Object accent = expenseItemList.get(0).get(ICmpConstant.ACCENTITY);
        List<String> accentityList = AuthUtil.getBizObjectAttr(transfereeInformation, ICmpConstant.INTO_ACCENTITY);
        Set<String> orgIds = FIDubboUtils.getDelegateHasSelf(accentityList.toArray(new String[0]));
        if (!"666666".equals(accent) && !orgIds.contains(accent.toString())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101935"),getMessage("UID:P_CM-BE_19435B1804F00022", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19435B1804F00022", "当前明细行导入的费用项目不在转入会计主体下！") /* "当前明细行导入的费用项目不在转入会计主体下！" */));
        }
    }

    private void ensureSubDataApportionmentMethod(TransfereeInformation transfereeInformation) throws CtmException {
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(transfereeInformation.getApportionmentMethod())).throwMessage(getMessage("UID:P_CM-BE_1942999405580056", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1942999405580056", "子表的分摊方式不能为空！") /* "子表的分摊方式不能为空！" */));
        Short apportionmentMethod = transfereeInformation.getApportionmentMethod();
        if (apportionmentMethod == ApportionmentMethod.PRORATED.getValue()) {
            BigDecimal apportionmentRatio = transfereeInformation.getApportionmentRatio();
            DetermineUtils.isTure(ValueUtils.isNotEmptyObj(apportionmentRatio)).throwMessage(getMessage("UID:P_CM-BE_194299940558005B", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_194299940558005B", "子表按比例分摊时，分摊比例不能为空！") /* "子表按比例分摊时，分摊比例不能为空！" */));
            transfereeInformation.setApportionmentRatio(apportionmentRatio.setScale(ICmpConstant.CONSTANT_TWO, RoundingMode.HALF_UP));
            transfereeInformation.setFixedAmount(null);
        } else if (apportionmentMethod == ApportionmentMethod.FIXED_AMOUNT.getValue()) {
            DetermineUtils.isTure(ValueUtils.isNotEmptyObj(transfereeInformation.getFixedAmount())).throwMessage(getMessage("UID:P_CM-BE_194299940558005E", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_194299940558005E", "子表按固定金额分摊时，固定金额不能为空！") /* "子表按固定金额分摊时，固定金额不能为空！" */));
            transfereeInformation.setApportionmentRatio(null);
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101936"),getMessage("UID:P_CM-BE_1942999405580060", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1942999405580060", "该分摊方式的枚举值不存在!") /* "该分摊方式的枚举值不存在!" */));
        }
    }

    private void validateEnterpriseParamsSub(BizObject bizObject, TransfereeInformation transfereeInformation, String enterpriseBankAccountSub) throws Exception {
        EnterpriseParams enterpriseParamsSub = new EnterpriseParams();
        enterpriseParamsSub.setId(enterpriseBankAccountSub);
        enterpriseParamsSub.setOrgid(transfereeInformation.getIntoAccentity());
        enterpriseParamsSub.setCurrencyIDList(Collections.singletonList(bizObject.get(ICmpConstant.CURRENCY).toString()));
        List<EnterpriseBankAcctVO> bankAccountsSub = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParamsSub);

        boolean flag = getaVoid(bizObject, transfereeInformation, enterpriseBankAccountSub);


        if (CollectionUtils.isNotEmpty(bankAccountsSub) || !flag) {
            validateBankAccountsSub(bizObject, transfereeInformation, bankAccountsSub);
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101937"),getMessage("UID:P_CM-BE_19435B1804F00020", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19435B1804F00020", "当前明细行导入的银行账户的币种与主表的币种不相等！") /* "当前明细行导入的银行账户的币种与主表的币种不相等！" */)+ bizObject.getString("code"));
        }
    }

    private Boolean getaVoid(BizObject bizObject, TransfereeInformation transfereeInformation, String enterpriseBankAccountSub) throws Exception {
        //转出账户分配 为“随前端业务” 时，不需要校验
        if(ValueUtils.isNotEmptyObj(bizObject.get("transferOutAccountAllocation")) && bizObject.getShort("transferOutAccountAllocation") == 0 ){
            return false;
        }
        EnterpriseBankAcctVOWithRange enterpriseBankAcctVoWithRange = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById(bizObject.get("enterprisebankaccount"));
        if(enterpriseBankAcctVoWithRange != null){
            List<OrgRangeVO> orgRangeVOS = enterpriseBankAcctVoWithRange.getAccountApplyRange();
            // 使用范围中的组织是否是授权的组织
            String accentity = bizObject.getString(IBussinessConstant.ACCENTITY);
            Set<String> rangeOrgIdList = new HashSet<>();
            for(OrgRangeVO orgRangeVO : orgRangeVOS){
                String rangeOrgId = orgRangeVO.getRangeOrgId();
                rangeOrgIdList.add(rangeOrgId);
            }
            if(!rangeOrgIdList.contains(accentity)){
                return false;
            }

            EnterpriseParams enterpriseParamsSub2 = new EnterpriseParams();
            enterpriseParamsSub2.setId(enterpriseBankAccountSub);
            enterpriseParamsSub2.setOrgid(transfereeInformation.getIntoAccentity());
            List<EnterpriseBankAcctVO> bankAccountsSub2 = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParamsSub2);


            EnterpriseParams enterpriseParamsSub1 = new EnterpriseParams();
            enterpriseParamsSub1.setOrgidList(new ArrayList<>(rangeOrgIdList));
            List<EnterpriseBankAcctVO> bankAccountsSub1 = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParamsSub1);

            Set<String> currencyIDs1 = new HashSet<>();
            for (EnterpriseBankAcctVO enterpriseBankAcctVO : bankAccountsSub2) {
                for (BankAcctCurrencyVO bankAcctCurrencyVO : enterpriseBankAcctVO.getCurrencyList()) {
                    currencyIDs1.add(bankAcctCurrencyVO.getCurrencyName());
                }
            }
            Set<String> currencyIDs2 = new HashSet<>();
            for (EnterpriseBankAcctVO enterpriseBankAcctVO : bankAccountsSub1) {
                for (BankAcctCurrencyVO bankAcctCurrencyVO : enterpriseBankAcctVO.getCurrencyList()) {
                    currencyIDs2.add(bankAcctCurrencyVO.getCurrencyName());
                }
            }
            return !currencyIDs2.containsAll(currencyIDs1);
        }
        return false;
    }

    private void validateBankAccountsSub(BizObject bizObject, TransfereeInformation transfereeInformation, List<EnterpriseBankAcctVO> bankAccountsSub) throws CtmException {
        if (CollectionUtils.isNotEmpty(bankAccountsSub)){
            EnterpriseBankAcctVO enterpriseBankAcctVO = bankAccountsSub.get(0);
            if (!transfereeInformation.getIntoAccentity().equals(enterpriseBankAcctVO.getOrgid())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101938"),getMessage("UID:P_CM-BE_19435B1804F00024", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19435B1804F00024", "当前明细行导入的银行账户不在转入会计主体下！") /* "当前明细行导入的银行账户不在转入会计主体下！" */));
            }
            Integer acctOpenType = enterpriseBankAcctVO.getAcctopentype();
            Short acctOpenTypeMain = bizObject.getShort(ICmpConstant.ACCT_OPEN_TYPE);
            if (!ValueUtils.isNotEmptyObj(acctOpenType)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100245"),getMessage("UID:P_CM-BE_1932EBC204080080", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_194D491804F0000F", "明细行上填写的银行账号对应的开户类型为空！") /* "明细行上填写的银行账号对应的开户类型为空！" */));
            }
            if (ValueUtils.isNotEmptyObj(acctOpenTypeMain) && !acctOpenTypeMain.toString().equals(acctOpenType.toString())) {
                transfereeInformation.set(ICmpConstant.ENTERPRISE_BANK_ACCOUNT, null);
                transfereeInformation.set(ICmpConstant.ENTERPRISEBANKACCOUNT_ACCOUNT, null);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101256"),getMessage("UID:P_CM-BE_1932EBC204080082", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_194D491804F00010", "转出方和转入方账户商业银行或结算中心账户的银行类别需要一致！") /* "转出方和转入方账户商业银行或结算中心账户的银行类别需要一致！" */));
            }
            transfereeInformation.set(ICmpConstant.ACCT_OPEN_TYPE, acctOpenType);
        }
    }

    private String getMessage(String uId, String defaultMessage) {
        return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault(uId, defaultMessage);
    }

    private void validateApportionmentRatioSum(List<TransfereeInformation> transfereeInformationList) {
        BigDecimal apportionmentRatioSum = transfereeInformationList.stream()
                .filter(item -> item.getApportionmentMethod() == ApportionmentMethod.PRORATED.getValue())
                .map(item -> item.getBigDecimal(ICmpConstant.APPORTIONMENT_RATIO))
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
        if (apportionmentRatioSum.compareTo(new BigDecimal(ICmpConstant.SELECT_HUNDRED_PARAM_INTEGER)) > ICmpConstant.CONSTANT_ZERO) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101939"),getMessage("UID:P_CM-BE_1942999405580059", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1942999405580059", "内转协议明细行上的分摊比例总和不能大于100!") /* "内转协议明细行上的分摊比例总和不能大于100!" */) /* "内转协议明细行上的分摊比例总和不能大于100!" */);
        }
    }

    private void setConstantData(BizObject bizObject) {
        bizObject.set(ICmpConstant.BILLTYPE, EventType.InternalTransferProtocol.getValue());
        bizObject.set(ICmpConstant.DATA_SOURCES, DataSources.THIRD_PARTY.getValue());
        bizObject.set(ICmpConstant.IS_DISCARD, (short) 0);
    }

}
