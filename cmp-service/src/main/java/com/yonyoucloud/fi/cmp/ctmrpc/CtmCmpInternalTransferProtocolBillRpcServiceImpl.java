package com.yonyoucloud.fi.cmp.ctmrpc;

import com.alibaba.fastjson.JSONObject;
import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.FIBillService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.api.ctmrpc.CtmCmpInternalTransferProtocolBillRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.event.utils.DetermineUtils;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.InternalTransferProtocol;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.InternalTransferProtocolVO;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.TransfereeInformation;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.enums.ApportionmentMethod;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.enums.DataSources;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.enums.TransferOutAccountAllocation;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.service.InternalTransferProtocolService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.internaltransferprotocol.CtmCmpInternalTransferProtocolBillV1RpcService;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h1>资金收付款单对外提供RPC接口</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023-02-28 19:06
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CtmCmpInternalTransferProtocolBillRpcServiceImpl implements CtmCmpInternalTransferProtocolBillRpcService, CtmCmpInternalTransferProtocolBillV1RpcService {

    private final FIBillService fiBillService;

    private final BaseRefRpcService baseRefRpcService;

    private final InternalTransferProtocolService internalTransferProtocolService;


    /**
     * <h2>保存来源于建筑云的内转协议单据接口</h2>
     *
     * @return java.lang.Object
     * @author Sun GuoCai
     * @since 2023/2/28 19:07
     */
    @Override
    public Object saveInternalTransferProtocolBill(CtmJSONObject params) throws Exception {
        log.error("save internal transfer protocol bill input parameter, data={}", CtmJSONObject.toJSONString(params));
        String data = (String) params.get(ICmpConstant.DATA);
        if (!ValueUtils.isNotEmptyObj(data)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100683"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807CC", "单据的数据不能为空！") /* "单据的数据不能为空！" */);
        }
        // 校验数据
        BizObject biz = verifyData(data);
        BillDataDto dataDto = new BillDataDto();
        Map<String, Object> map = new HashMap<>();
        dataDto.setBillnum(IBillNumConstant.INTERESTRATESETTINGLIST);
        dataDto.setData(CtmJSONObject.toJSONString(biz));
        RuleExecuteResult ruleExecuteResult = fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), dataDto);
        BizObject result = (BizObject) ruleExecuteResult.getData();
        Long id = result.getLong(ICmpConstant.PRIMARY_ID);
        map.put(ICmpConstant.PRIMARY_ID, id);
        YtsContext.setYtsContext("SAVE_INTERNAL_TRANSFER_PROTOCOL_BILL", map);
        Map<String, Object> resultResponse = new HashMap<>();
        resultResponse.put(ICmpConstant.IS_SUCCESS, true);
        resultResponse.put(ICmpConstant.BILLTYPE, EventType.InternalTransferProtocol.getValue());
        resultResponse.put(ICmpConstant.PRIMARY_ID, id);
        ruleExecuteResult.setOutParams(resultResponse);
        return ruleExecuteResult;
    }


    private BizObject verifyData(String data) throws Exception {
        BizObject bizObject = new BizObject();
        CtmJSONObject jsonObject = CtmJSONObject.parseObject(data);
        bizObject.putAll(jsonObject);
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.ACCENTITY))).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19429A7604F00013", "主表的会计主体不能为空！") /* "主表的会计主体不能为空！" */);
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.PROJECT))).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19429A7604F00014", "主表的项目不能为空！") /* "主表的项目不能为空！" */);
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.IS_ENABLED_TYPE))).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19429A7604F00016", "主表的启停用状态不能为空！") /* "主表的启停用状态不能为空！" */);
        Short transferOutAccountAllocation = bizObject.getShort(ICmpConstant.TRANSFER_OUT_ACCOUNT_ALLOCATION);
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(transferOutAccountAllocation)).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19429A7604F00018", "主表的转出账户分配不能为空！") /* "主表的转出账户分配不能为空！" */);
        if (transferOutAccountAllocation == TransferOutAccountAllocation.WITH_FRONT_END_BUSINESS.getValue()) {
            DetermineUtils.isTure(ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.CURRENCY))).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19429A7604F00019", "主表的币种不能为空！") /* "主表的币种不能为空！" */);
        } else if (transferOutAccountAllocation == TransferOutAccountAllocation.SETUP_ACCOUNT_MANUALLY.getValue()) {
            DetermineUtils.isTure(ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.ENTERPRISE_BANK_ACCOUNT_LOWER))).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19429A7604F0001B", "主表的银行账户不能为空！") /* "主表的银行账户不能为空！" */);
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101271"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19429A7604F0001D", "该转出账户分配的枚举值不存在!") /* "该转出账户分配的枚举值不存在!" */);
        }
        String enterpriseBankAccount = bizObject.getString(ICmpConstant.ENTERPRISE_BANK_ACCOUNT_LOWER);
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setId(enterpriseBankAccount);
        List<EnterpriseBankAcctVO> bankAccounts = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
        if (!bankAccounts.isEmpty()) {
            CmpCommonUtil.queryBankAcctVOByParams(bizObject, bankAccounts);
        }

        List<TransfereeInformation> transfereeInformationList = bizObject.getBizObjects("TransfereeInformation", TransfereeInformation.class);
        for (TransfereeInformation transfereeInformation : transfereeInformationList) {
            DetermineUtils.isTure(ValueUtils.isNotEmptyObj(transfereeInformation.getIntoAccentity())).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19429A7604F00010", "子表的转入会计主体不能为空！") /* "子表的转入会计主体不能为空！" */);
            DetermineUtils.isTure(ValueUtils.isNotEmptyObj(transfereeInformation.getExpenseitem())).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19429A7604F00011", "子表的费用项目不能为空！") /* "子表的费用项目不能为空！" */);
            DetermineUtils.isTure(ValueUtils.isNotEmptyObj(transfereeInformation.getApportionmentMethod())).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19429A7604F00012", "子表的分摊方式不能为空！") /* "子表的分摊方式不能为空！" */);
            Short apportionmentMethod = transfereeInformation.getApportionmentMethod();
            if (apportionmentMethod == ApportionmentMethod.PRORATED.getValue()) {
                BigDecimal apportionmentRatio = transfereeInformation.getApportionmentRatio();
                DetermineUtils.isTure(ValueUtils.isNotEmptyObj(apportionmentRatio)).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19429A7604F00017", "子表按比例分摊时，分摊比例不能为空！") /* "子表按比例分摊时，分摊比例不能为空！" */);
                transfereeInformation.setApportionmentRatio(apportionmentRatio.setScale(ICmpConstant.CONSTANT_TWO, RoundingMode.HALF_UP));
            } else if (apportionmentMethod == ApportionmentMethod.FIXED_AMOUNT.getValue()) {
                DetermineUtils.isTure(ValueUtils.isNotEmptyObj(transfereeInformation.getFixedAmount())).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19429A7604F0001A", "子表按固定金额分摊时，固定金额不能为空！") /* "子表按固定金额分摊时，固定金额不能为空！" */);
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101272"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19429A7604F0001C", "该分摊方式的枚举值不存在!") /* "该分摊方式的枚举值不存在!" */);
            }
            String enterpriseBankAccountSub = transfereeInformation.getEnterpriseBankAccount();
            DetermineUtils.isTure(ValueUtils.isNotEmptyObj(enterpriseBankAccountSub)).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19429A7604F0001E", "子表的银行账户不能为空！") /* "子表的银行账户不能为空！" */);
            EnterpriseParams enterpriseParamsSub = new EnterpriseParams();
            enterpriseParamsSub.setId(enterpriseBankAccount);
            List<EnterpriseBankAcctVO> bankAccountsSub = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParamsSub);
            if (!bankAccountsSub.isEmpty()) {
                EnterpriseBankAcctVO enterpriseBankAcctVO = bankAccountsSub.get(0);
                Integer acctOpenType = enterpriseBankAcctVO.getAcctopentype();
                Short acctOpenTypeMain = bizObject.getShort(ICmpConstant.ACCT_OPEN_TYPE);
                if (!ValueUtils.isNotEmptyObj(acctOpenType)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100245"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC204080080", "明细行上填写的银行账号对应的开户类型为空！") /* "明细行上填写的银行账号对应的开户类型为空！" */);
                }
                // 转出方和转入方的账户，需要银行类别同时为商业银行账户或结算中心账户。否则提示：转出方和转入方账户商业银行或结算中心账户的银行类别需要一致
                CmpCommonUtil.verifyAcctOpenTypeIsSame(transfereeInformation, acctOpenType, acctOpenTypeMain);
            }
        }
        BigDecimal apportionmentRatioSum = transfereeInformationList.stream().map(item -> item.getBigDecimal(ICmpConstant.APPORTIONMENT_RATIO)).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        if (apportionmentRatioSum.compareTo(new BigDecimal(ICmpConstant.SELECT_HUNDRED_PARAM_INTEGER)) > ICmpConstant.CONSTANT_ZERO) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101273"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19429A7604F00015", "内转协议明细行上的分摊比例总和不能大于100!") /* "内转协议明细行上的分摊比例总和不能大于100!" */);
        }
        CmpCommonUtil.billCodeHandler(bizObject, InternalTransferProtocol.ENTITY_NAME, ICmpConstant.CMP_POINT_INTERNALTRANSFERPROTOCOL);
        // 设置常量
        bizObject.set(ICmpConstant.BILLTYPE, EventType.InternalTransferProtocol.getValue());
        bizObject.set(ICmpConstant.DATA_SOURCES, DataSources.MANUALLY_ADD.getValue());
        bizObject.set(ICmpConstant.IS_DISCARD, (short) 0);
        return bizObject;
    }


    /**
     * <h2>保存来源于建筑云的内转协议单据回滚接口</h2>
     *
     * @return java.lang.Object
     * @author Sun GuoCai
     * @since 2023/2/28 19:07
     */
    @Override
    public Object rollBackSaveInternalTransferProtocolBill(CtmJSONObject params) throws Exception {
        log.error("rollBackSaveInternalTransferProtocolBill, data={}", CtmJSONObject.toJSONString(params));
        Map<String, Object> map = collectRows(YtsContext.getYtsContext("SAVE_INTERNAL_TRANSFER_PROTOCOL_BILL"));
        log.error("rollBackSaveInternalTransferProtocolBill=> data={}", map.toString());
        BillDataDto dataDto = new BillDataDto();
        CtmJSONObject json = new CtmJSONObject();
        json.put(ICmpConstant.PRIMARY_ID, Long.parseLong(map.get(ICmpConstant.PRIMARY_ID).toString()));
        dataDto.setData(CtmJSONObject.toJSONString(json));
        dataDto.setAction(ICmpConstant.DELETE);
        dataDto.setBillnum(IBillNumConstant.INTERESTRATESETTINGLIST);
        return fiBillService.executeUpdate(OperationTypeEnum.DELETE.getValue(), dataDto).getData();
    }

    private static Map<String, Object> collectRows(Object map) {
        Map<String, Object> mapResult = new HashMap<>();
        if (map instanceof Map<?, ?>){
            for (Map.Entry<?, ?> entry:((Map<?, ?>) map).entrySet()){
                mapResult = createMapResult(entry);
            }
        }
        return mapResult;
    }

    private static Map<String, Object> createMapResult(Map.Entry<?, ?> entry) {
        Map<String, Object> mapResult = new HashMap<>();
        if(entry.getKey() instanceof String ){
            mapResult.put((String) entry.getKey(), entry.getValue());
        }
        return mapResult;
    }

    /**
     * <h2>删除来源于建筑云的内转协议单据接口</h2>
     *
     * @return java.lang.Object
     * @author Sun GuoCai
     * @since 2023/2/28 19:07
     */
    @Override
    public Object deleteInternalTransferProtocolBill(CtmJSONObject params) throws Exception {
        log.error("delete internal transfer protocol bill input parameter, data={}", CtmJSONObject.toJSONString(params));
        Object id = params.get(ICmpConstant.PRIMARY_ID);
        if (!ValueUtils.isNotEmptyObj(id)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101274"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807C8", "单据id不能为空！") /* "单据id不能为空！" */);
        }

        BizObject bizObject = MetaDaoHelper.findById(InternalTransferProtocol.ENTITY_NAME, id, 2);
        if (!ValueUtils.isNotEmptyObj(bizObject)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101275"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807CA", "根据id未查询到单据！") /* "根据id未查询到单据！" */);
        }
        if (bizObject.getShort(ICmpConstant.DATA_SOURCES) != DataSources.CONSTRUCTION_CLOUD.getValue()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101276"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19429A7604F0000F", "非建筑云生成的内转协议单，无法删除") /* "非建筑云生成的内转协议单，无法删除" */);
        }
        BillDataDto dataDto = new BillDataDto();
        dataDto.setData(bizObject);
        Map<String, Object> rollBackMap = new HashMap<>();

        dataDto.setBillnum(IBillNumConstant.CMP_INTERNALTRANSFERPROTOCOL);
        rollBackMap.put(ICmpConstant.FULL_NAME, InternalTransferProtocol.ENTITY_NAME);

        rollBackMap.put(ICmpConstant.DATA, bizObject);
        bizObject.set(ICmpConstant.IS_DISCARD, (short)1);
        bizObject.setEntityStatus(EntityStatus.Update);
        RuleExecuteResult ruleExecuteResult = fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), dataDto);
        YtsContext.setYtsContext("DELETE_INTERNAL_TRANSFER_PROTOCOL_BILL", rollBackMap);
        return ruleExecuteResult;
    }


    /**
     * <h2>删除来源于建筑云的内转协议单据回滚接口</h2>
     *
     * @return java.lang.Object
     * @author Sun GuoCai
     * @since 2023/2/28 19:07
     */
    @Override
    public Object rollBackDeleteInternalTransferProtocolBill(CtmJSONObject params) throws Exception {
        log.error("rollBackDeleteInternalTransferProtocolBill, data={}", CtmJSONObject.toJSONString(params));
        Map<String, Object> map = collectRows(YtsContext.getYtsContext("DELETE_INTERNAL_TRANSFER_PROTOCOL_BILL"));
        log.error("rollBackDeleteInternalTransferProtocolBill=> map={}", map.toString());
        BizObject bizObject = (BizObject) map.get(ICmpConstant.DATA);
        bizObject.set(ICmpConstant.IS_DISCARD, (short)0);
        BillDataDto dataDto = new BillDataDto();
        dataDto.setData(map.get(ICmpConstant.DATA));
        dataDto.setAction(ICmpConstant.SAVE);
        dataDto.setBillnum(IBillNumConstant.CMP_INTERNALTRANSFERPROTOCOL);
        return fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), dataDto).getData();
    }

    @Override
    public Object queryValidInternalTransferBill(JSONObject params) throws Exception {
        String accentity = params.getString(ICmpConstant.ACCENTITY);//会计主体
        String currency = params.getString(ICmpConstant.CURRENCY);//币种
        String enterpriseBankAccount = params.getString(ICmpConstant.ENTERPRISE_BANK_ACCOUNT_LOWER);//银行账号
        String project = params.getString(ICmpConstant.PROJECT);//项目
        InternalTransferProtocolVO internalTransferProtocolVO = new InternalTransferProtocolVO();
        internalTransferProtocolVO.setAccentity(accentity);
        internalTransferProtocolVO.setProject(project);
        internalTransferProtocolVO.setCurrency(currency);
        internalTransferProtocolVO.setEnterpriseBankAccount(enterpriseBankAccount);
        return internalTransferProtocolService.queryValidInternalTransferBill(internalTransferProtocolVO);
    }

    @Override
    public Object internalTransferBillGeneratesFundPaymentBill(JSONObject params) throws Exception {
        InternalTransferProtocolVO internalTransferProtocolVO = params.getObject(ICmpConstant.CMP_POINT_INTERNALTRANSFERPROTOCOL, InternalTransferProtocolVO.class);
        return internalTransferProtocolService.internalTransferBillGeneratesFundPaymentBill(internalTransferProtocolVO);
    }

}
