package com.yonyoucloud.fi.cmp.internaltransferprotocol.service.impl;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodQueryParam;
import com.yonyou.iuap.event.model.BusinessEvent;
import com.yonyou.iuap.event.model.BusinessEventBuilder;
import com.yonyou.iuap.event.rpc.IEventSendService;
import com.yonyou.iuap.log.cons.OperCodeTypes;
import com.yonyou.iuap.log.model.BusinessObject;
import com.yonyou.iuap.log.rpc.IBusinessLogService;
import com.yonyou.iuap.log.util.BusiObjectBuildUtil;
import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.iuap.org.dto.FundsOrgDTO;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.bpm.service.ProcessService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyou.yonbip.ctm.orgs.FundsOrgQueryServiceComponent;
import com.yonyoucloud.fi.basecom.itf.IFIBillService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.CurrencyUtil;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.CaObject;
import com.yonyoucloud.fi.cmp.cmpentity.EntryType;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.cmpentity.IsEnable;
import com.yonyoucloud.fi.cmp.cmpentity.QuickTypeVO;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.common.pushAndPull.PushAndPullService;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IMsgConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.event.utils.DetermineUtils;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.InternalTransferProtocol;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.InternalTransferProtocolVO;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.ProtocolCallLogs;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.TransfereeInformation;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.enums.ApportionmentMethod;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.enums.DataSources;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.enums.TransferOutAccountAllocation;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.service.InternalTransferProtocolService;
import com.yonyoucloud.fi.cmp.pushAndPull.PushAndPullModel;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import com.yonyoucloud.fi.cmp.vo.ResultMessageVO;
import com.yonyoucloud.fi.fieaai.busievent.dto.v1.EventMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * <h1>内转协议服务实现类</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023-09-08 16:19
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class InternalTransferProtocolServiceImpl implements InternalTransferProtocolService {
    private final CTMCMPBusinessLogService ctmcmpBusinessLogService;
    private final BaseRefRpcService baseRefRpcService;
    private final PushAndPullService pushAndPullService;
    private final CmCommonService<Object> commonService;
    //    private final OrgRpcService orgRpcService;
    private final ProcessService processService;
    private final YmsOidGenerator ymsOidGenerator;
    private final CmCommonService<Object> cmCommonService;
    private final IFIBillService fiBillService;
    private final IEventSendService eventSendService;
    private final FundsOrgQueryServiceComponent fundsOrgQueryServiceComponent;

    /**
     * <h2>更新内转协议单据的启停用状态</h2>
     *
     * @param ids           :
     * @param isEnabledType :
     * @return ResultMessageVO<java.lang.String>
     * @author Sun GuoCai
     * @since 2023/9/10 9:54
     */
    @Override
    public ResultMessageVO<String> updateEnabledStatusOfTransferProtocolByIds(String ids, String isEnabledType) throws CtmException {
        ResultMessageVO<String> result = new ResultMessageVO<>();
        List<InternalTransferProtocol> internalTransferProtocolList = Lists.newArrayList();
        log.error("Switch bill status input parameter. ids={}, isEnabledType={}", ids, isEnabledType);
        try {
            List<String> idsList = Arrays.stream(ids.split(",")).collect(Collectors.toList());
            List<BizObject> mapList = new ArrayList<>();
            for (String id : idsList) {
                BizObject object = MetaDaoHelper.findById(InternalTransferProtocol.ENTITY_NAME, id, 2);
                mapList.add(object);
            }
            for (BizObject map : mapList) {
                InternalTransferProtocol internalTransferProtocol = new InternalTransferProtocol();
                internalTransferProtocol.init(map);
                if ("1".equals(isEnabledType)) {
                    internalTransferProtocol.setIsEnabledType(IsEnable.DISENABLE.getValue());
                } else {
                    // 启用时，校验明细行银行账号必填
                    List<TransfereeInformation> transfereeInformationList = internalTransferProtocol.getBizObjects(ICmpConstant.TRANSFEREE_INFORMATION, TransfereeInformation.class);
                    boolean match = transfereeInformationList.stream().anyMatch(item -> !ValueUtils.isNotEmptyObj(item.getEnterpriseBankAccount()));
                    if (match) {
                        String msgError = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_197A6E1404C00007", "内转协议单据[%s]，存在银行账号没填的明细行，请检查！") /* "内转协议单据[%s]，存在银行账号没填的明细行，请检查！" */, internalTransferProtocol.getCode());
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101940"), msgError);
                    }
                    // 已废弃的单据不允许在启用
                    Short isDiscard = internalTransferProtocol.getShort(ICmpConstant.IS_DISCARD);
                    if (isDiscard == (short) 1) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101941"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_197D05D004980005", "已废弃的单据不允许在启用！") /* "已废弃的单据不允许在启用！" */);
                    }
                    internalTransferProtocol.setIsEnabledType(IsEnable.ENABLE.getValue());
                }
                internalTransferProtocol.setEntityStatus(EntityStatus.Update);
                internalTransferProtocolList.add(internalTransferProtocol);
            }
            MetaDaoHelper.update(InternalTransferProtocol.ENTITY_NAME, internalTransferProtocolList);
            String message;
            if ("1".equals(isEnabledType)) {
                message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC20408008E", "单据停用成功!") /* "单据停用成功!" */;
                ctmcmpBusinessLogService.saveBusinessLog(internalTransferProtocolList, internalTransferProtocolList.get(0).getCode(), "", IServicecodeConstant.INTERNAL_TRANSFER_PROTOCOL_SERVICE_CODE, IMsgConstant.CMP_INTERNAL_TRANSFER_PROTOCOL, IMsgConstant.CMP_ENABLE);
            } else {
                message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC20408008F", "单据启用成功!") /* "单据启用成功!" */;
                ctmcmpBusinessLogService.saveBusinessLog(internalTransferProtocolList, internalTransferProtocolList.get(0).getCode(), "", IServicecodeConstant.INTERNAL_TRANSFER_PROTOCOL_SERVICE_CODE, IMsgConstant.CMP_INTERNAL_TRANSFER_PROTOCOL, IMsgConstant.CMP_UNENABLE);
            }
            result.setMsg(message);
            return result;
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101942"), e.getMessage());
        }
    }

    /**
     * <h2>认领单结合内转协议单生成资金付款单</h2>
     *
     * @param internalTransferProtocolVO : 认领单入参
     * @return CtmJSONObject
     * @author Sun GuoCai
     * @since 2023/9/11 14:55
     */
    @Override
    public ResultMessageVO<String> internalTransferBillGeneratesFundPaymentBill(InternalTransferProtocolVO internalTransferProtocolVO) {
        CtmJSONObject ctmJSONObject = new CtmJSONObject();
        BizObject sendMsgBizObj = null;
        ResultMessageVO<String> result = new ResultMessageVO<>();
        ctmJSONObject.put("1.internalTransferProtocolVO", internalTransferProtocolVO);
        ctmJSONObject.put("code", internalTransferProtocolVO.getSrcBillCode());
        // 1.校验参数
        dataVerify(internalTransferProtocolVO);
        try {
            // 2.查询有效协议
            List<Map<String, Object>> list = new ArrayList<>();
            if (StringUtils.isEmpty(internalTransferProtocolVO.getProtocolId())) {
                list = internalTransferProtocolVO.getValidInternalTransferBills();
                if (CollectionUtils.isEmpty(list)) {
                    list = getValidData(internalTransferProtocolVO);
                }
                ctmJSONObject.put("2.validProtocol", list);
            }

            // 3.整理转单前协议数据
            BizObject bizObj = getBizObject(internalTransferProtocolVO, list);
            ctmJSONObject.put("3.protocolToBConverted", bizObj);
            ctmJSONObject.put("code", bizObj.get(ICmpConstant.CODE));

            // 3.1整理转单前协议数据 根据表头、表体银行账号是否i相同对数据进行分组
            BizObject[] bizObjs = divideBizObject(bizObj);
            bizObj = bizObjs[0];
            sendMsgBizObj = bizObjs[1];
            ctmJSONObject.put("3.1.pushFundPaymentBizObj", bizObj);
            ctmJSONObject.put("3.2.sendMsgBizObj", sendMsgBizObj);

            if (null != bizObj) {
                // 4.开始进行单据转换
                BizObject fundPayment = convertFundPaymentBill(internalTransferProtocolVO, bizObj);
                ctmJSONObject.put("4.afterConversionFundPayment", fundPayment);

                // 5.加工并保存提交资金付款单
                RuleExecuteResult ruleExecuteResult = processedAndSaveFundPaymentData(fundPayment);

                BizObject fundPaymentSaved = null;
                if (ruleExecuteResult.getData() instanceof BizObject) {
                    log.error("加工并保存提交资金付款单 BizObject:" + CtmJSONObject.toJSONString(ruleExecuteResult.getData()));
                    fundPaymentSaved = (BizObject) ruleExecuteResult.getData();
                } else if (ruleExecuteResult.getData() instanceof HashMap) {
                    log.error("加工并保存提交资金付款单 HashMap:" + CtmJSONObject.toJSONString(ruleExecuteResult.getData()));
                    fundPaymentSaved = new BizObject((HashMap) ruleExecuteResult.getData());
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101943"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508007E", "加工并保存提交资金付款单异常！") /* "加工并保存提交资金付款单异常！" */);
                }
                ctmJSONObject.put("5.processedAndSaveAndSubmitFundPayment", CtmJSONObject.toJSONString(ruleExecuteResult));

                // 6.保存协议调用日志【保存并提交成功或保存成功提交失败】
                if (ValueUtils.isNotEmptyObj(fundPaymentSaved)) {
                    ProtocolCallLogs protocolCallLogs = saveProtocolCallLogs(fundPaymentSaved, bizObj, internalTransferProtocolVO);
                    ctmJSONObject.put("6.1.protocolCallLogs", protocolCallLogs);
                }
                ctmJSONObject.put("7.1.msgSuccess", HttpStatus.OK.getReasonPhrase());
                result.setCode(ICmpConstant.REQUEST_SUCCESS_STATUS_CODE);
                result.setMsg(HttpStatus.OK.getReasonPhrase());
            }

        } catch (Exception e) {
            ctmJSONObject.put("7.2.msgError", e.getMessage());
            saveBusinessLog(ctmJSONObject);
            log.error("Claim Form: Generate a payment slip for funds according to the internal transfer agreement, failed!", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101944"), e.getMessage());
        }

        if (null != sendMsgBizObj) {
            BusinessEventBuilder businessEventBuilder = new BusinessEventBuilder();
            businessEventBuilder.setSourceId(ICmpConstant.INTERNALTRANS_EVENT_SOURCE);
            businessEventBuilder.setEventType(ICmpConstant.INTERNALTRANS_EVENT_TYPE_01);
            businessEventBuilder.setBillId(sendMsgBizObj.get(ICmpConstant.ID));
            businessEventBuilder.setBillno(sendMsgBizObj.get(ICmpConstant.CODE));
            businessEventBuilder.setBillCode(sendMsgBizObj.get(ICmpConstant.CODE));
            List<EventMessageDTO> eventMessages = new ArrayList<>();
            EventMessageDTO eventMessageDTO = new EventMessageDTO();
            sendMsgBizObj.put(ICmpConstant.CMP_POINT_INTERNALTRANSFERPROTOCOL, internalTransferProtocolVO);
            eventMessageDTO.setData(sendMsgBizObj);
            eventMessages.add(eventMessageDTO);
            businessEventBuilder.setUserObject(eventMessages);
            businessEventBuilder.setTenantCode(AppContext.getCurrentUser().getYTenantId());
            BusinessEvent businessEvent = businessEventBuilder.build();
            eventSendService.sendEvent(businessEvent);
            ctmJSONObject.put(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054006C8", "8.发送消息") /* "8.发送消息" */, businessEvent);
        }

        saveBusinessLog(ctmJSONObject);
        return result;
    }

    /**
     * <h2>OpenAPI:根据id删除内转协议单</h2>
     *
     * @param param :
     * @return ResultMessageVO<java.lang.Object>
     * @author Sun GuoCai
     * @since 2023/9/19 11:56
     */
    @Override
    public ResultMessageVO<Object> deleteInternalTransferBillByIds(BillDataDto param) {
        Object data = param.getData();
        List<Map<String, Long>> rows = getListIds(data);
        List<String> messages = new ArrayList<>();
        int failedCount = 0;
        List<Long> ids = new ArrayList<>();
        Map<Long, InternalTransferProtocol> protocolBillMap = new HashMap<>();
        for (Map<String, Long> rowData : rows) {
            Long id = rowData.get("id");
            ids.add(id);
        }
        try {
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
            queryConditionGroup.appendCondition(QueryCondition.name("id").in((Object) ids));
            querySchema.addCondition(queryConditionGroup);
            List<InternalTransferProtocol> internalTransferProtocolList = MetaDaoHelper.queryObject(InternalTransferProtocol.ENTITY_NAME, querySchema, null);
            for (InternalTransferProtocol internalTransferProtocol : internalTransferProtocolList) {
                protocolBillMap.put(Long.parseLong(internalTransferProtocol.getId()), internalTransferProtocol);
            }
            List<InternalTransferProtocol> updateProtocolList = new ArrayList<>(rows.size());
            for (int i = 0; i < rows.size(); i++) {
                Map<String, Long> row = rows.get(i);
                InternalTransferProtocol internalTransferProtocol = protocolBillMap.get(row.get("id"));
                boolean raisedError = false;
                if (!ValueUtils.isNotEmptyObj(internalTransferProtocol)) {
                    messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807D5", "单据不存在 id:") /* "单据不存在 id:" */ + rows.get(i).get("id").toString());
                    raisedError = true;
                } else if (internalTransferProtocol.getShort("dataSources") != DataSources.THIRD_PARTY.getValue()) {
                    messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_194D478E04F00011", "单据数据来源不是第三方 id:") /* "单据数据来源不是第三方 id:" */ + rows.get(i).get("id").toString());
                    raisedError = true;
                } else if (internalTransferProtocol.getIsEnabledType() == IsEnable.ENABLE.getValue()) {
                    messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_194D95440558000D", "单据状态只有是停用状态才允许删除 id:") /* "单据状态只有是停用状态才允许删除 id:" */ + rows.get(i).get("id").toString());
                    raisedError = true;
                }
                if (raisedError) {
                    failedCount++;
                    if (rows.size() == 1) {
                        return getJsonObject(rows, messages, failedCount);
                    }
                    continue;
                }
                internalTransferProtocol.put("isDiscard", (short) 1);
                updateProtocolList.add(internalTransferProtocol);
            }

            if (ValueUtils.isNotEmptyObj(updateProtocolList)) {
                EntityTool.setUpdateStatus(updateProtocolList);
                MetaDaoHelper.update(InternalTransferProtocol.ENTITY_NAME, updateProtocolList);
            }
            return getJsonObject(rows, messages, failedCount);
        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101945"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_194D478E04F00012", "删除内转协议单据失败！") /* "删除内转协议单据失败！" */);
        }
    }

    @Override
    public ResultMessageVO<Map<String, Object>> queryValidInternalTransferBill(InternalTransferProtocolVO internalTransferProtocolVO) throws CtmException {
        ResultMessageVO<Map<String, Object>> result = new ResultMessageVO<>();
        try {
            //查询有效的补充协议
            List<Map<String, Object>> list = getValidData(internalTransferProtocolVO);
            result.setRows(list);
            result.setCode(ICmpConstant.REQUEST_SUCCESS_STATUS_CODE);
            result.setMsg(HttpStatus.OK.getReasonPhrase());
        } catch (Exception e) {
            log.error("查询有效补充协议报错,具体错误信息如下：", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101946"), e.getMessage());
        }
        return result;
    }


    /**
     * 对内转协议进行分组
     * （1）如果表头、表体银行账号相同，构造推资金付款单的内转协议
     * （2）如果表头、表体的银行账号不同，构造发送消息的内转协议（河北建工场景）
     *
     * @param internalTransferProtocol 内转协议
     * @return
     */
    private BizObject[] divideBizObject(BizObject internalTransferProtocol) {
        BizObject[] bizObjects = new BizObject[]{null, null};
        //推资金付款单内转协议明细
        BizObject fundPaymentBizObject = internalTransferProtocol.clone();
        List<BizObject> fundPaymentDetails = new ArrayList<>();
        //发送消息内转协议明细
        BizObject sendMsgBizObject = internalTransferProtocol.clone();
        List<BizObject> sendMsgDetails = new ArrayList<>();
        List<BizObject> transfereeInformationList = internalTransferProtocol.get(ICmpConstant.TRANSFEREE_INFORMATION);
        if (CollectionUtils.isNotEmpty(transfereeInformationList)) {
            //主表银行账号
            String enterpriseBankAccount = JSON.parseObject(JSON.toJSONString(internalTransferProtocol)).getString(ICmpConstant.INTERNALTRANS_ENTERPRISE_BANK_ACCOUNT);
            for (BizObject transfereeInformation : transfereeInformationList) {
                Boolean isauto = transfereeInformation.getBoolean(ICmpConstant.IS_AUTO);
                //没有是否自动这个字段没有值 或者 是否自动这个字段为是
                if (!ValueUtils.isNotEmptyObj(transfereeInformation.get(ICmpConstant.IS_AUTO))
                        || isauto) {
                    //子表银行账号
                    String subEnterpriseBankAccount = transfereeInformation.getString(ICmpConstant.ENTERPRISE_BANK_ACCOUNT);
                    //子表银行账号为空 或者 子表银行账号 = 主表银行账号
                    if (StringUtils.isEmpty(subEnterpriseBankAccount)
                            || !subEnterpriseBankAccount.equals(enterpriseBankAccount)) {
                        fundPaymentDetails.add(transfereeInformation);
                    } else {
                        sendMsgDetails.add(transfereeInformation);
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(fundPaymentDetails)) {
                fundPaymentBizObject.put(ICmpConstant.TRANSFEREE_INFORMATION, fundPaymentDetails);
                bizObjects[0] = fundPaymentBizObject;
            }
            if (CollectionUtils.isNotEmpty(sendMsgDetails)) {
                sendMsgBizObject.put(ICmpConstant.TRANSFEREE_INFORMATION, sendMsgDetails);
                bizObjects[1] = sendMsgBizObject;
            }
        }
        return bizObjects;
    }

    @NotNull
    private static List<Map<String, Long>> getListIds(Object data) {
        List<Map<String, Long>> rows = new ArrayList<>();
        if (data instanceof List<?>) {
            for (Object map : (List<?>) data) {
                rows.addAll(collectRows(map));
            }
        }
        return rows;
    }

    private static Collection<? extends Map<String, Long>> collectRows(Object map) {
        List<Map<String, Long>> rows = new ArrayList<>();
        if (map instanceof Map<?, ?>) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) map).entrySet()) {
                Map<String, Long> mapResult = createMapResult(entry);
                if (!mapResult.isEmpty()) {
                    rows.add(mapResult);
                }
            }
        }
        return rows;
    }

    private static Map<String, Long> createMapResult(Map.Entry<?, ?> entry) {
        Map<String, Long> mapResult = new HashMap<>();
        if (entry.getKey() instanceof String && entry.getValue() instanceof Long) {
            mapResult.put((String) entry.getKey(), (Long) entry.getValue());
        }
        return mapResult;
    }

    private ResultMessageVO<Object> getJsonObject(List<Map<String, Long>> rows, List<String> messages, int failedCount) {
        ResultMessageVO<Object> responseData = new ResultMessageVO<>();
        responseData.setCount(rows.size());
        responseData.setSuccessCount(rows.size() - failedCount);
        responseData.setFailCount(failedCount);
        responseData.setMessages(messages);
        responseData.setInfos(rows);
        return responseData;
    }

    @NotNull
    private ProtocolCallLogs saveProtocolCallLogs(BizObject fundPaymentSaved,
                                                  BizObject internalTransferProtocol,
                                                  InternalTransferProtocolVO internalTransferProtocolVO) {
        try {
            String fundPaymentCode = fundPaymentSaved.getString(ICmpConstant.CODE);
            ProtocolCallLogs protocolCallLogs = new ProtocolCallLogs();
            protocolCallLogs.setCallerProtocolVersion(internalTransferProtocol.get(ICmpConstant.VERSION_NO));
            protocolCallLogs.setCallDate(new Date());
            protocolCallLogs.setCallerSystemName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC204080089", "现金管理") /* "现金管理" */);
            protocolCallLogs.setGeneratedDocumentStatus(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC20408008A", "已保存") /* "已保存" */);
            protocolCallLogs.setGeneratedDocumentCode(fundPaymentCode);
            protocolCallLogs.setGeneratedDocumentId(Long.parseLong(fundPaymentSaved.getId().toString()));
            protocolCallLogs.setCallerTransactionType(null);
            protocolCallLogs.setCallerDocumentCode(internalTransferProtocolVO.getSrcBillCode());
            long protocolCallLogsId = ymsOidGenerator.nextId();
            protocolCallLogs.setId(protocolCallLogsId);
            protocolCallLogs.setMainid(internalTransferProtocol.getId().toString());
            protocolCallLogs.setEntityStatus(EntityStatus.Insert);
            CmpMetaDaoHelper.insert(ProtocolCallLogs.ENTITY_NAME, protocolCallLogs);
            return protocolCallLogs;
        } catch (Exception e) {
            log.error("saveProtocolCallLogs fail! errorMsg={}", e.getMessage());
            return new ProtocolCallLogs();
        }
    }


    private RuleExecuteResult processedAndSaveFundPaymentData(BizObject fundPayment) throws Exception {
        processExchangeRates(fundPayment);
        processPayments(fundPayment);
        handleBillCodeIfNeeded(fundPayment);
        handleTradeTypeIfNeeded(fundPayment);
        setupOtherAttributes(fundPayment);
        return executeRule(fundPayment);
    }

    private void processExchangeRates(BizObject fundPayment) throws Exception {
        String accent = fundPayment.getString(ICmpConstant.ACCENTITY);
        FinOrgDTO finOrgDTO = AccentityUtil.getFinOrgDTOByAccentityId(accent);
        CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(finOrgDTO.getCurrency());
        String natCurrency = currencyTenantDTO.getId();
        fundPayment.set(ICmpConstant.NATCURRENCY, natCurrency);
        // 汇率类型
        Map<String, Object> defaultExchangeRateType = commonService.getDefaultExchangeRateType(fundPayment.getString(ICmpConstant.ACCENTITY));
        if (defaultExchangeRateType != null && defaultExchangeRateType.get(ICmpConstant.PRIMARY_ID) != null) {
            fundPayment.set(ICmpConstant.EXCHANGE_RATE_TYPE, defaultExchangeRateType.get(ICmpConstant.PRIMARY_ID));
            fundPayment.set(ICmpConstant.EXCHANGE_RATE_TYPE_DIGIT, defaultExchangeRateType.get(ICmpConstant.DIGIT));
        }
        Object currency = fundPayment.get(ICmpConstant.CURRENCY);
        // 汇率（取汇率表中报价日期小于等于单据日期的值）
        boolean isCurrencyFlag = currency.equals(natCurrency);
        if (isCurrencyFlag) {
            fundPayment.set(ICmpConstant.EXCHRATE, ICmpConstant.CONSTANT_ONE);
            fundPayment.set(ICmpConstant.EXCHRATEOPS, 1);
        } else {
            /*Double currencyRateNew = CurrencyUtil.getCurrencyRateNew(null,
                    fundPayment.get(ICmpConstant.EXCHANGE_RATE_TYPE),
                    fundPayment.get(ICmpConstant.CURRENCY),
                    natCurrency, fundPayment.get(ICmpConstant.VOUCHDATE), fundPayment.get(ICmpConstant.EXCHANGE_RATE_TYPE_DIGIT));*/
            CmpExchangeRateVO cmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(fundPayment.get(ICmpConstant.CURRENCY), natCurrency, fundPayment.get(ICmpConstant.VOUCHDATE), fundPayment.get(ICmpConstant.EXCHANGE_RATE_TYPE), fundPayment.get(ICmpConstant.EXCHANGE_RATE_TYPE_DIGIT));
            if (cmpExchangeRateVO == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100682"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041807CD", "币种找不到汇率") /* "币种找不到汇率" */);
            }
            fundPayment.set(ICmpConstant.EXCHRATE,cmpExchangeRateVO.getExchangeRate().setScale(fundPayment.get("exchangeRateType_digit"), RoundingMode.HALF_UP));
            fundPayment.set(ICmpConstant.EXCHRATEOPS,cmpExchangeRateVO.getExchangeRateOps());
        }
    }

    private void processPayments(BizObject fundPayment) throws Exception {
        List<BizObject> fundPaymentBList = fundPayment.get(ICmpConstant.FUND_PAYMENT_B);
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal lineno = new BigDecimal(ICmpConstant.CONSTANT_TEN);
        Object currency = fundPayment.get(ICmpConstant.CURRENCY);
        Object natCurrency = fundPayment.get(ICmpConstant.NATCURRENCY);
        boolean isCurrencyFlag = currency.equals(natCurrency);
        for (BizObject bizObject : fundPaymentBList) {
            bizObject.set(ICmpConstant.CURRENCY, currency);
            bizObject.set(ICmpConstant.NATCURRENCY, natCurrency);
            sum = CmpCommonUtil.getBigDecimal(fundPayment, sum, isCurrencyFlag, bizObject);
            Long quickType = setQuickType(bizObject);
            Long settleMode = setSettleMode(bizObject);
            if (ValueUtils.isNotEmptyObj(quickType)) {
                bizObject.put(ICmpConstant.QUICK_TYPE, quickType);
            }
            if (ValueUtils.isNotEmptyObj(settleMode)) {
                bizObject.put(ICmpConstant.SETTLE_MODE, settleMode);
            }
            //FinOrgDTO finOrgDto = AccentityUtil.getFinOrgDTOByAccentityId(bizObject.get(ICmpConstant.OPPOSITEOBJECTID));
            FundsOrgDTO fundsOrgDTO = fundsOrgQueryServiceComponent.getById(bizObject.get(ICmpConstant.OPPOSITEOBJECTID));
            bizObject.put(ICmpConstant.OPPOSITEOBJECTNAME, fundsOrgDTO.getName());

            EnterpriseParams enterpriseParams = new EnterpriseParams();
            enterpriseParams.setId(bizObject.get(ICmpConstant.OPPOSITEACCOUNTID));
            List<EnterpriseBankAcctVO> bankAccounts = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
            if (!bankAccounts.isEmpty()) {
                EnterpriseBankAcctVO enterpriseBankAcctVO = bankAccounts.get(0);
                bizObject.set(ICmpConstant.OPPOSITEACCOUNTNAME, enterpriseBankAcctVO.getAcctName());
                bizObject.set(ICmpConstant.OPPOSITEACCOUNTNO, enterpriseBankAcctVO.getAccount());
                bizObject.set(ICmpConstant.OPPOSITEBANKADDRID, enterpriseBankAcctVO.getBankNumber());
                bizObject.set(ICmpConstant.OPPOSITEBANKADDR, enterpriseBankAcctVO.getBankNumberName());
                bizObject.set(ICmpConstant.OPPOSITEBANKLINENO, enterpriseBankAcctVO.getLineNumber());
                bizObject.set(ICmpConstant.OPPOSITEBANKTYPE, enterpriseBankAcctVO.getBankName());
            }
            bizObject.set("settleCurrency", currency);
            bizObject.set("swapOutExchangeRateEstimate", new BigDecimal(1));
            bizObject.set("swapOutAmountEstimate", bizObject.get(ICmpConstant.ORISUM));
            bizObject.set("swapOutExchangeRateType", fundPayment.get(ICmpConstant.EXCHANGE_RATE_TYPE));

            bizObject.put(ICmpConstant.CA_OBJECT, CaObject.InnerUnit.getValue());
            bizObject.put(ICmpConstant.SETTLE_STATUS, FundSettleStatus.WaitSettle.getValue());
            bizObject.put(ICmpConstant.LINE_NO, lineno);
            bizObject.set(ICmpConstant.EXCHANGE_RATE_TYPE, fundPayment.get(ICmpConstant.EXCHANGE_RATE_TYPE));
            lineno = BigDecimalUtils.safeAdd(lineno, new BigDecimal(ICmpConstant.CONSTANT_TEN));
        }
        fundPayment.put(ICmpConstant.FUND_PAYMENT_B, fundPaymentBList);
        CmpCommonUtil.organizeAmountData(fundPayment, sum, isCurrencyFlag);
        fundPayment.set(ICmpConstant.VOUCHDATE, DateUtils.dateTimeToDate(new Date()));
        fundPayment.set(ICmpConstant.ENTRY_TYPE, EntryType.Normal_Entry.getValue());
    }

    @Nullable
    private Long setSettleMode(BizObject bizObject) {
        Long settleMode = null;
        if (!ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.SETTLE_MODE))) {
            SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
            settleMethodQueryParam.setCode(ICmpConstant.SETTLE_MODE_BANK_TRANSFER_CODE);
            settleMethodQueryParam.setIsEnabled(ICmpConstant.CONSTANT_ONE);
            List<SettleMethodModel> dataList = baseRefRpcService.querySettleMethods(settleMethodQueryParam);
            if (!dataList.isEmpty()) {
                settleMode = dataList.get(0).getId();
            }
        } else {
            settleMode = bizObject.getLong(ICmpConstant.SETTLE_MODE);
        }
        return settleMode;
    }

    @Nullable
    private Long setQuickType(BizObject bizObject) throws Exception {
        Long quickType = null;
        if (!ValueUtils.isNotEmptyObj(bizObject.get(ICmpConstant.QUICK_TYPE))) {
            String quickCode = "9";
            HashMap<String, Object> quickCodeMap = SqlHelper.selectOne(ICmpConstant.QUICK_TYPE_MAPPER_FUND_PAYMENT_BILL, AppContext.getTenantId());
            if (null != quickCodeMap && null != quickCodeMap.get(ICmpConstant.C_DEFAULT_VALUE) && !"".equals(quickCodeMap.get(ICmpConstant.C_DEFAULT_VALUE))) {
                quickCode = String.valueOf(quickCodeMap.get(ICmpConstant.C_DEFAULT_VALUE));
            }
            QuickTypeVO quickTypeVO = CmpCommonUtil.queryQuickTypeByCode(quickCode);
            if (ValueUtils.isNotEmptyObj(quickTypeVO)) {
                quickType = quickTypeVO.getId();

            }
        } else {
            quickType = bizObject.getLong(ICmpConstant.QUICK_TYPE);
        }
        return quickType;
    }

    private void handleBillCodeIfNeeded(BizObject fundPayment) {
        if (!ValueUtils.isNotEmptyObj(fundPayment.get(ICmpConstant.CODE))) {
            CmpCommonUtil.billCodeHandler(fundPayment, FundPayment.ENTITY_NAME, IBillNumConstant.FUND_PAYMENT);
        }
    }

    private void handleTradeTypeIfNeeded(BizObject fundPayment) {
        if (!ValueUtils.isNotEmptyObj(fundPayment.get(ICmpConstant.TRADE_TYPE))) {
            tradeTypeHandler(fundPayment);
        }
    }

    private void setupOtherAttributes(BizObject fundPayment) {
        // 事项来源
        fundPayment.set(ICmpConstant.SRC_ITEM, EventSource.Cmpchase.getValue());
        // 事项类型
        fundPayment.set(ICmpConstant.BILLTYPE, EventType.InternalTransferProtocol.getValue());
        // 审批状态
        fundPayment.set(ICmpConstant.AUDIT_STATUS, AuditStatus.Incomplete.getValue());
        // 审批流状态
        fundPayment.set(ICmpConstant.VERIFY_STATE, VerifyState.INIT_NEW_OPEN.getValue());
        // 凭证状态
        fundPayment.set(ICmpConstant.VOUCHER_STATUS, VoucherStatus.Empty.getValue());
    }

    private RuleExecuteResult executeRule(BizObject fundPayment) throws Exception {
        RuleExecuteResult result;
        try {
            BillContext billContext = CmpCommonUtil.getBillContextByFundPayment();
            boolean isWfControlled = processService.bpmControl(billContext, fundPayment);
            fundPayment.put(ICmpConstant.IS_WFCONTROLLED, isWfControlled);
        } catch (Exception e) {
            fundPayment.put(ICmpConstant.IS_WFCONTROLLED, false);
        }
        BillDataDto dataDto = new BillDataDto();
        dataDto.setBillnum(IBillNumConstant.FUND_PAYMENT);
        dataDto.setData(CtmJSONObject.toJSONString(fundPayment));
        int autoSubmit = ValueUtils.isNotEmptyObj(fundPayment.get("autoSubmit"))
                ? fundPayment.getInteger("autoSubmit") : 0;
        if (autoSubmit == 1) {
            result = cmCommonService.doSaveAndSubmitAction(dataDto);
            // 注意这里判断result是否正常结束的状态，1:代表保存并提交成功；999：代表保存失败；910：代表保存成功但提交失败
            if (1 != result.getMsgCode()) {
                if (999 == result.getMsgCode()) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101947"), result.getMessage());
                } else {
                    return result;
                }
            } else {
                return result;
            }
        } else {
            result = fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), dataDto);
        }
        return result;
    }

    private BizObject convertFundPaymentBill(InternalTransferProtocolVO internalTransferProtocolVO, BizObject bizObj) throws Exception {
        if (!ValueUtils.isNotEmptyObj(bizObj)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101920"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC204080086", "传入的单据为空，请检查！") /* "传入的单据为空，请检查！" */);
        }
        PushAndPullModel pushAndPullModel = new PushAndPullModel();
        pushAndPullModel.setCode(ICmpConstant.INTERNAL_TRANSFER_PROTOCOL_CONVERT_FUND_PAYMENT);
        pushAndPullModel.setQuerydb(false);
        bizObj.set("billClaimId", internalTransferProtocolVO.getSrcBillId());
        List<BizObject> bizObjects = pushAndPullService.transformBillByMakeBillCodeAll(Collections.singletonList(bizObj), pushAndPullModel);
        if (bizObjects.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101920"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC204080086", "传入的单据为空，请检查！") /* "传入的单据为空，请检查！" */);
        } else {
            return bizObjects.get(0);
        }
    }

    @NotNull
    private BizObject getBizObject(InternalTransferProtocolVO internalTransferProtocolVO, List<Map<String, Object>> list) {
        BizObject bizObj;
        if (!ValueUtils.isNotEmptyObj(internalTransferProtocolVO.getProtocolId())) {
            bizObj = initMainData(list);
            validateAccountInfo(internalTransferProtocolVO, bizObj);
            processSubData(internalTransferProtocolVO, bizObj);
        } else {
            try {
                bizObj = MetaDaoHelper.findById(InternalTransferProtocol.ENTITY_NAME, internalTransferProtocolVO.getProtocolId(), 2);
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101948"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508007F", "根据切块逻辑传过来的内转协议id查询内转协议单据信息失败!") /* "根据切块逻辑传过来的内转协议id查询内转协议单据信息失败!" */);
            }
        }
        return bizObj;
    }

    private BizObject initMainData(List<Map<String, Object>> list) {
        BizObject bizObj = new BizObject();
        List<BizObject> subList = new ArrayList<>();
        for (Map<String, Object> data : list) {
            BizObject object = new BizObject();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                assignValuesToBizObject(bizObj, object, entry);
            }
            subList.add(object);
        }
        bizObj.put(ICmpConstant.TRANSFEREE_INFORMATION, subList);
        return bizObj;
    }

    private void assignValuesToBizObject(BizObject mainBizObject, BizObject subBizObject, Map.Entry<String, Object> entry) {
        String key = entry.getKey();
        if (key.contains(ICmpConstant.TRANSFEREE_INFORMATION_)) {
            subBizObject.put(key.substring(key.indexOf("_") + ICmpConstant.CONSTANT_ONE), entry.getValue());
        } else {
            mainBizObject.put(key, entry.getValue());
        }
    }

    private void validateAccountInfo(InternalTransferProtocolVO internalTransferProtocolVO, BizObject bizObj) {
        Short transferOutAccountAllocation = bizObj.getShort(ICmpConstant.TRANSFER_OUT_ACCOUNT_ALLOCATION);
        if (transferOutAccountAllocation == TransferOutAccountAllocation.WITH_FRONT_END_BUSINESS.getValue() && !ValueUtils.isNotEmptyObj(internalTransferProtocolVO.getEnterpriseBankAccount())) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101949"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC204080088", "无有效内转协议!"));
        }

        if (transferOutAccountAllocation == TransferOutAccountAllocation.WITH_FRONT_END_BUSINESS.getValue()) {
            bizObj.put(ICmpConstant.ENTERPRISE_BANK_ACCOUNT_LOWER, internalTransferProtocolVO.getEnterpriseBankAccount());
        }
    }

    private void processSubData(InternalTransferProtocolVO internalTransferProtocolVO, BizObject bizObj) {
        List<BizObject> transfereeInformationList = bizObj.get(ICmpConstant.TRANSFEREE_INFORMATION);
        List<BizObject> proratedList = transfereeInformationList.stream().filter(item -> item.getShort(ICmpConstant.APPORTIONMENT_METHOD) == ApportionmentMethod.PRORATED.getValue()).collect(Collectors.toList());
        if (proratedList.isEmpty()) return;
        BigDecimal splitAmount = internalTransferProtocolVO.getSplitAmount();
        processRatio(proratedList, splitAmount);
    }

    private void processRatio(List<BizObject> proratedList, BigDecimal splitAmount) {
        BigDecimal apportionmentRatioSum = proratedList.stream().map(item -> item.getBigDecimal(ICmpConstant.APPORTIONMENT_RATIO)).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        if (apportionmentRatioSum.compareTo(new BigDecimal(ICmpConstant.SELECT_HUNDRED_PARAM_INTEGER)) == ICmpConstant.CONSTANT_ZERO) {
            processSumRatio(proratedList, splitAmount);
        } else {
            proratedList.forEach(bizObject -> setFixedAmount(bizObject, splitAmount, bizObject.getBigDecimal(ICmpConstant.APPORTIONMENT_RATIO)));
        }
    }

    private void processSumRatio(List<BizObject> proratedList, BigDecimal splitAmount) {
        int flag = ICmpConstant.CONSTANT_ONE;
        BigDecimal sum = BigDecimal.ZERO;
        for (BizObject bizObject : proratedList) {
            if (flag == proratedList.size()) {
                BigDecimal amount = BigDecimalUtils.safeSubtract(splitAmount, sum).setScale(ICmpConstant.CONSTANT_TWO, RoundingMode.HALF_UP);
                bizObject.set(ICmpConstant.FIXED_AMOUNT, amount);
            } else {
                BigDecimal decimal = setFixedAmount(bizObject, splitAmount, bizObject.getBigDecimal(ICmpConstant.APPORTIONMENT_RATIO));
                sum = BigDecimalUtils.safeAdd(sum, decimal);
            }
            flag++;
        }
    }

    private BigDecimal setFixedAmount(BizObject bizObject, BigDecimal splitAmount, BigDecimal apportionmentRatio) {
        BigDecimal decimal = BigDecimalUtils.safeMultiply(BigDecimalUtils.safeDivide(apportionmentRatio,
                        new BigDecimal(ICmpConstant.SELECT_HUNDRED_PARAM_INTEGER), ICmpConstant.CONSTANT_TEN), splitAmount)
                .setScale(ICmpConstant.CONSTANT_TWO, RoundingMode.HALF_UP);
        bizObject.set(ICmpConstant.FIXED_AMOUNT, decimal);
        return decimal;
    }

    /**
     * 根据条件查询有效的内转协议
     *
     * @param internalTransferProtocolVO
     * @return
     * @throws Exception
     */
    @NotNull
    private List<Map<String, Object>> getValidData(InternalTransferProtocolVO internalTransferProtocolVO) throws Exception {
        // 根据传入的会计主体以及项目，过滤未停用的内转协议
        QuerySchema schema = QuerySchema.create().addSelect("*,TransfereeInformation.*");
        QueryConditionGroup queryConditionGroup;

        // 如果内转协议编号不为空，则根据编号查询内转协议，否则按照条件查询内转协议
        if (StringUtils.isNotEmpty(internalTransferProtocolVO.getContractNo())) {
            queryConditionGroup = QueryConditionGroup.and(
                    QueryCondition.name(ICmpConstant.CODE).eq(internalTransferProtocolVO.getContractNo()),
                    QueryCondition.name(ICmpConstant.IS_ENABLED_TYPE).eq(IsEnable.ENABLE.getValue()),
                    QueryCondition.name(ICmpConstant.IS_DISCARD).eq(ICmpConstant.CONSTANT_ZERO));
            schema.appendQueryCondition(queryConditionGroup);
            List<Map<String, Object>> list = MetaDaoHelper.query(InternalTransferProtocol.ENTITY_NAME, schema);
            if (CollectionUtils.isEmpty(list)) {
                return list;
            }
        } else {
            queryConditionGroup = QueryConditionGroup.and(QueryCondition.name(ICmpConstant.ACCENTITY).eq(internalTransferProtocolVO.getAccentity()),
                    QueryCondition.name(ICmpConstant.PROJECT).eq(internalTransferProtocolVO.getProject()),
                    QueryCondition.name(ICmpConstant.IS_ENABLED_TYPE).eq(IsEnable.ENABLE.getValue()),
                    QueryCondition.name(ICmpConstant.CURRENCY).eq(internalTransferProtocolVO.getCurrency()),
                    QueryCondition.name(ICmpConstant.IS_PARENT).eq(ICmpConstant.CONSTANT_ONE),
                    QueryCondition.name(ICmpConstant.IS_DISCARD).eq(ICmpConstant.CONSTANT_ZERO)
            );
        }


        // 如果传入了银行账户，则判断银行账户开户类型
        if (ValueUtils.isNotEmptyObj(internalTransferProtocolVO.getEnterpriseBankAccount()) && !ValueUtils.isNotEmptyObj(internalTransferProtocolVO.getAcctOpenType())) {
            EnterpriseParams enterpriseParams = new EnterpriseParams();
            enterpriseParams.setId(internalTransferProtocolVO.getEnterpriseBankAccount());
            List<EnterpriseBankAcctVO> bankAccounts = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
            if (!bankAccounts.isEmpty()) {
                EnterpriseBankAcctVO enterpriseBankAcctVO = bankAccounts.get(0);
                Integer acctOpenType = enterpriseBankAcctVO.getAcctopentype();
                if (!ValueUtils.isNotEmptyObj(acctOpenType)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101950"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC204080091", "银行账户对应的开户类型为空！") /* "银行账户对应的开户类型为空！" */);
                }
                internalTransferProtocolVO.setAcctOpenType(Short.parseShort(acctOpenType.toString()));
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101951"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC204080092", "银行账户不存在！") /* "银行账户不存在！" */);
            }
        }
        schema.appendQueryCondition(queryConditionGroup);
        List<Map<String, Object>> list = MetaDaoHelper.query(InternalTransferProtocol.ENTITY_NAME, schema);
        if (list.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101949"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC204080088", "无有效内转协议!") /* "无有效内转协议!" */);
        }
        return list;
    }

    private void tradeTypeHandler(BizObject biz) {
        String formId = ICmpConstant.CM_CMP_FUND_PAYMENT;
        String tradeTypeCode = "cmp_fund_payment_other";

        String billTypeId = null;
        try {
            List<Map<String, Object>> list = CmpCommonUtil.setTradeTypeByCode(formId);
            if (CollectionUtils.isNotEmpty(list)) {
                Map<String, Object> objectMap = list.get(0);
                if (!ValueUtils.isNotEmptyObj(objectMap)) {
                    log.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC20408008C", "查询资金付款单交易类型失败！请检查数据！") /* "查询资金付款单交易类型失败！请检查数据！" */);
                }
                billTypeId = MapUtils.getString(objectMap, ICmpConstant.PRIMARY_ID);
            }
            Map<String, Object> tradetypeMap = commonService.queryTransTypeById(billTypeId, "0", tradeTypeCode);
            if (ValueUtils.isNotEmptyObj(tradetypeMap)) {
                biz.set(ICmpConstant.TRADE_TYPE, tradetypeMap.get(ICmpConstant.PRIMARY_ID));
            }
        } catch (Exception e) {
            log.error(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC204080090", "未获取到默认的交易类型！, billTypeId = {}, e = {}") /* "未获取到默认的交易类型！, billTypeId = {}, e = {}" */, billTypeId, e.getMessage());
        }
    }


    private void dataVerify(InternalTransferProtocolVO internalTransferProtocolVO) {
        log.error("dataVerify, input parameter internalTransferProtocolVO={}", CtmJSONObject.toJSONString(internalTransferProtocolVO));
        // 返给事项平台的参数校验
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(internalTransferProtocolVO)).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC204080093", "内转协议生成资金付款单，请求参数实体为空！") /* "内转协议生成资金付款单，请求参数实体为空！" */);
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(internalTransferProtocolVO.getAccentity())).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC204080094", "内转协议生成资金付款单，请求参数的会计主体为空！") /* "内转协议生成资金付款单，请求参数的会计主体为空！" */);
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(internalTransferProtocolVO.getProject())).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC204080095", "内转协议生成资金付款单，请求参数的项目为空！") /* "内转协议生成资金付款单，请求参数的项目为空！" */);
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(internalTransferProtocolVO.getCurrency())).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC204080096", "内转协议生成资金付款单，请求参数的币种为空！") /* "内转协议生成资金付款单，请求参数的币种为空！" */);
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(internalTransferProtocolVO.getSplitAmount())).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC204080097", "内转协议生成资金付款单，请求参数的可拆分金额为空！") /* "内转协议生成资金付款单，请求参数的可拆分金额为空！" */);
        DetermineUtils.isTure(ValueUtils.isNotEmptyObj(internalTransferProtocolVO.getSrcBillId())).throwMessage(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1932EBC204080098", "内转协议生成资金付款单，请求参数的来源单据ID为空！") /* "内转协议生成资金付款单，请求参数的来源单据ID为空！" */);
    }

    public void saveBusinessLog(CtmJSONObject ctmJSONObject) {
        try {
            BusinessObject businessObject = BusiObjectBuildUtil.build(IServicecodeConstant.INTERNAL_TRANSFER_PROTOCOL_SERVICE_CODE,
                    InternalTransferProtocol.ENTITY_NAME, OperCodeTypes.publish,
                    IMsgConstant.CMP_INTERNAL_TRANSFER_PROTOCOL, IMsgConstant.INTERNAL_TRANSFER_PROTOCOL_CONVERT_FUNDPAYMENT, ctmJSONObject);
            IBusinessLogService businessLogService = AppContext.getBean(IBusinessLogService.class);
            businessLogService.saveBusinessLog(businessObject);
        } catch (Exception var8) {
            log.error("记录业务日志失败：" + var8.getMessage());
        }
    }

}
