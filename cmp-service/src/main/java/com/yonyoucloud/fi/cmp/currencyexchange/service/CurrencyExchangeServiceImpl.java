package com.yonyoucloud.fi.cmp.currencyexchange.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import com.yonyou.iuap.billcode.service.IBillCodeSupport;
import com.yonyou.iuap.fileservice.sdk.module.CooperationFileService;
import com.yonyou.iuap.fileservice.sdk.module.pojo.CooperationFileInfo;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.uap.billcode.BillCodeComponentParam;
import com.yonyou.uap.billcode.BillCodeObj;
import com.yonyou.uap.billcode.service.IBillCodeComponentService;
import com.yonyou.ucf.basedoc.model.*;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.bpm.service.ProcessService;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.ucf.mdd.ext.voucher.enums.Status;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.autocorrsetting.ReWriteBusCorrDataService;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetCommonManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetCurrencyExchangeManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetManagerService;
import com.yonyoucloud.fi.cmp.budget.CmpBudgetVO;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.currencyapply.CurrencyApply;
import com.yonyoucloud.fi.cmp.currencyapply.service.CurrencyApplyService;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchangeCharacterDef;
import com.yonyoucloud.fi.cmp.enums.BillMapEnum;
import com.yonyoucloud.fi.cmp.exchangesettlement.ExchangeSettlementPurpose;
import com.yonyoucloud.fi.cmp.exchangesettlement.ExchangeSettlementTradeCode;
import com.yonyoucloud.fi.cmp.exchangesourcecode.ExchangeSourceCode;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journal.JournalService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.settlement.Settlement;
import com.yonyoucloud.fi.cmp.settlement.service.SettlementService;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.TransTypeQueryService;
import com.yonyoucloud.fi.cmp.vo.ExchangeDeliveryRequestVO;
import com.yonyoucloud.fi.cmp.vo.FinanceCompanyRateQueryRequestVO;
import com.yonyoucloud.fi.cmp.voucher.CmpVoucherService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.message.BasicNameValuePair;
import org.imeta.biz.base.Objectlizer;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yonyou.bpm.rest.RepositoryService;
import yonyou.bpm.rest.request.repository.ProcessDefinitionQueryParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper.findById;

/**
 * 外币兑换实现类
 *
 * @author dongjch 2019年9月10日
 */
@Service
@Slf4j
@Transactional(rollbackFor = RuntimeException.class)
public class CurrencyExchangeServiceImpl implements CurrencyExchangeService {

    private final static String SETTLE_FLAG = "settleflag";

    private final static String SETTLEMENT_DATE = "settlementdate";

    // 财务公司的银行类别编码
    private final static String FINANCE_COMPANY_BANK_TYPE_CODE = "system-002";

    @Autowired
    private JournalService journalService;

    @Autowired
    CmpVoucherService cmpVoucherService;

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Autowired
    private BankConnectionAdapterContext bankConnectionAdapterContext;

    @Autowired
    YmsOidGenerator ymsOidGenerator;

    @Autowired
    CurrencyExchangeManager currencyExchangeManager;

    @Autowired
    CmCommonService cmCommonService;

    @Autowired
    BankAccountSettingService bankAccountSettingService;

    @Autowired
    CurrencyApplyService currencyApplyService;

    @Autowired
    ProcessService processService;

    @Autowired
    TransTypeQueryService transTypeQueryService;

    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    CurrencyQueryService currencyQueryService;

    @Autowired
    private CmpBudgetManagerService cmpBudgetManagerService;

    @Autowired
    private CmpBudgetCommonManagerService cmpBudgetCommonManagerService;

    @Autowired
    private CmpBudgetCurrencyExchangeManagerService cmpBudgetCurrencyExchangeManagerService;


    @Autowired
    CmpWriteBankaccUtils cmpWriteBankaccUtils;

    @Autowired
    ReWriteBusCorrDataService reWriteBusCorrDataService;

    @Override
    public List<CurrencyExchange> queryByIds(Long[] ids) throws Exception {
        List<CurrencyExchange> currencyExchangeList = null;
        List<Map<String, Object>> currencyExchangeMapList = MetaDaoHelper.queryByIds(CurrencyExchange.ENTITY_NAME, ICmpConstant.SELECT_TOTAL_PARAM, ids);
        if (CollectionUtils.isNotEmpty(currencyExchangeMapList)) {
            currencyExchangeList = new ArrayList<CurrencyExchange>();
            for (Map<String, Object> map : currencyExchangeMapList) {
                CurrencyExchange currencyExchange = new CurrencyExchange();
                currencyExchange.init(map);
                currencyExchangeList.add(currencyExchange);
            }
        }
        return currencyExchangeList;
    }

    @Override
    public CtmJSONObject settle(List<CurrencyExchange> currencyExchangeList) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        List<String> messages = new ArrayList<>();
        //最大日结日期
        Map<String, Date> maxSettleDateMaps = new HashMap<String, Date>();
        if (CollectionUtils.isEmpty(currencyExchangeList)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102483"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418072E", "请选择单据！") /* "请选择单据！" */);
        }
        Long[] ids = new Long[currencyExchangeList.size()];
        List<CurrencyExchange> currencyExchangeDBList = new ArrayList<>();
        for (int i = 0; i < currencyExchangeList.size(); i++) {
            Long idsss = currencyExchangeList.get(i).getId();
            CurrencyExchange currencyExchange = MetaDaoHelper.findById(CurrencyExchange.ENTITY_NAME, idsss);
            currencyExchangeDBList.add(currencyExchange);
        }
        CtmJSONObject failed = new CtmJSONObject();
        int i = 0;
        Date date = BillInfoUtils.getBusinessDate();
        if (currencyExchangeDBList.size() > 0) {
            for (CurrencyExchange currencyExchange : currencyExchangeDBList) {
                if (currencyExchange.getAuditstatus() != null && currencyExchange.getAuditstatus().getValue() == AuditStatus.Incomplete.getValue()) {
                    failed.put(currencyExchange.getId().toString(), currencyExchange.getId().toString());
                    // 单据【%s】未审批，不能进行交割！
                    messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180736", "单据[%s]未审批，不能进行交割！") /* "单据[%s]未审批，不能进行交割！" */, currencyExchange.getCode()));
                    if (currencyExchangeDBList.size() == 1) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102484"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189FAC8005C0000C", "该单据未审批，不能进行交割！") /* "该单据未审批，不能进行交割！" */);
                    }
                    i++;
                    continue;
                }
                if (currencyExchange.get("settlemode") == null) {
                    failed.put(currencyExchange.getId().toString(), currencyExchange.getId().toString());
                    //单据[%s]结算方式为空！，不能进行交割！
                    messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806F9", "单据[%s]结算方式为空！，不能进行交割！") /* "单据[%s]结算方式为空！，不能进行交割！" */, currencyExchange.getCode()));
                    if (currencyExchangeDBList.size() == 1) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102485"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806FC", "结算方式为空！") /* "结算方式为空！" */);
                    }
                    i++;
                    continue;
                }

                if (currencyExchange.getSettlestatus() != null && currencyExchange.getSettlestatus().getValue() == SettleStatus.alreadySettled.getValue()) {
                    failed.put(currencyExchange.getId().toString(), currencyExchange.getId().toString());
                    //单据[%s]已交割不能重复交割！
                    messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180704", "单据[%s]已交割不能重复交割！") /* "单据[%s]已交割不能重复交割！" */, currencyExchange.getCode()));
                    if (currencyExchangeDBList.size() == 1) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102486"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806F8", "单据已交割完成不能重复交割！") /* "单据已交割完成不能重复交割！" */);
                    }
                    i++;
                    continue;
                }
                //直联交割不能手工结算
                if (currencyExchange.getDeliveryType() != null && currencyExchange.getDeliveryType() == DeliveryType.DirectDelivery.getValue()) {
                    failed.put(currencyExchange.getId().toString(), currencyExchange.getId().toString());
                    messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806FA", "当前单据为直联单据，手工交割失败。") /* "当前单据为直联单据，手工交割失败。" */);
                    if (currencyExchangeDBList.size() == 1) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102487"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806FA", "当前单据为直联单据，手工交割失败。") /* "当前单据为直联单据，手工交割失败。" */);
                    }
                    i++;
                    continue;
                }

                //外币兑换申请过来的单据不赋值
                if (currencyExchange.getBilltype() == null) {
                    currencyExchange.set("billtype", EventType.CurrencyExchangeBill.getValue());
                }

                if (BillInfoUtils.getBusinessDate() != null) {
                    currencyExchange.setSettledate(BillInfoUtils.getBusinessDate());
                } else {
                    currencyExchange.setSettledate(AppContext.getLoginDate());
                }
//                if (currencyExchange.getSettledate().compareTo(currencyExchange.getAuditDate())<0){
//                    failed.put(currencyExchange.getId().toString(), currencyExchange.getId().toString());
//                    if (ids.length == 1) {
//                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102488"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180705","单据结算日期小于审核日期，不能交割") /* "单据结算日期小于审核日期，不能交割" */);
//                    }
//                    //单据[%s]结算日期小于审核日期，不能交割！
//                    messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180713","单据[%s]结算日期小于审核日期，不能交割！") /* "单据[%s]结算日期小于审核日期，不能交割！" */, currencyExchange.getCode()));
//                    i++;
//                    currencyExchange.setSettledate(null);
//                    continue;
//                }
                //校验结算日期是否已日结
                Date maxSettleDate = null;
                if (maxSettleDateMaps.containsKey(currencyExchange.getAccentity())) {
                    maxSettleDate = maxSettleDateMaps.get(currencyExchange.getAccentity());
                } else {
                    maxSettleDate = settlementService.getMaxSettleDate(currencyExchange.getAccentity());
                    maxSettleDateMaps.put(currencyExchange.getAccentity(), maxSettleDate);
                }
                if (SettleCheckUtil.checkDailySettlement(maxSettleDate, false)) {
                    failed.put(currencyExchange.getId().toString(), currencyExchange.getId().toString());
                    if (ids.length == 1) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102489"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180709", "当前日结日期为[%s]，交割业务日期不能小于等于日结日期！") /* "当前日结日期为[%s]，交割业务日期不能小于等于日结日期！" */, maxSettleDate));
                    }
                    messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806FD", "单据【") /* "单据【" */ + currencyExchange.getCode() + String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180709", "当前日结日期为[%s]，交割业务日期不能小于等于日结日期！") /* "当前日结日期为[%s]，交割业务日期不能小于等于日结日期！" */, maxSettleDate));
                    i++;
                    continue;
                }
                currencyExchange.set("_entityName", CurrencyExchange.ENTITY_NAME);
                CtmJSONObject generateResult = cmpVoucherService.generateVoucherWithResult(currencyExchange);
                if (!generateResult.getBoolean("dealSucceed")) {
                    failed.put(currencyExchange.getId().toString(), currencyExchange.getId().toString());
                    if (ids.length == 1) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101118"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418070C", "发送会计平台失败：") /* "发送会计平台失败：" */ + generateResult.get("message"));
                    }
                    messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180720", "单据[%s]发送会计平台失败，不能进行手工交割！") /* "单据[%s]发送会计平台失败，不能进行手工交割！" */, currencyExchange.getCode()));
                    i++;
                    currencyExchange.setSettledate(null);
                    continue;
                }
                //手工交割要修改为手工交割成功
                currencyExchange.setSettlestatus(DeliveryStatus.completeDelivery);
                boolean implement = cmpBudgetCurrencyExchangeManagerService.implement(currencyExchange);
                if (implement) {
                    currencyExchange.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
                }
                journalService.updateJournal(currencyExchange);
                EntityTool.setUpdateStatus(currencyExchange);
                MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, currencyExchange);
                if (currencyExchange.getBilltype() != null && currencyExchange.getBilltype() == EventType.CurrencyExchangeApply.getValue()) {
                    // 外币兑换申请，事项类型的单据，需更新外币申请单据状态
                    currencyApplyService.updateDeliveryStatus(currencyExchange.getCurrencyapplyid(),
                            DeliveryStatus.completeDelivery.getValue(), currencyExchange.getSettledate());
                }
                ctmcmpBusinessLogService.saveBusinessLog(currencyExchange, currencyExchange.getCode(), "", IServicecodeConstant.CURRENCYEXCHANGE, IMsgConstant.CURRENCY_EXCHANGE, IMsgConstant.SETTLE);
            }
        }
        String message = null;
        if (currencyExchangeDBList.size() == 1) {
            message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180725", "手工交割成功!") /* "手工交割成功!" */;
        } else {
            message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1818746205B0000B", "共：%s张单据；%s张手工交割成功；%s张手工交割失败！") /* "共：%s张单据；%s张手工交割成功；%s张手工交割失败！" */, currencyExchangeDBList.size(), (currencyExchangeDBList.size() - i), i);
//            message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180728","共：") /* "共：" */ + currencyExchangeDBList.size() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180727","张单据；") /* "张单据；" */ + (currencyExchangeDBList.size() - i) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180726","张手工交割成功；") /* "张手工交割成功；" */ + i + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180729","张手工交割失败！") /* "张手工交割失败！" */;
        }
        result.put("msg", message);
        result.put("msgs", messages);
        result.put("messages", messages);
        result.put("count", currencyExchangeDBList.size());
        result.put("sucessCount", currencyExchangeDBList.size() - i);
        result.put("failCount", i);
        if (failed.size() > 0) {
            result.put("failed", failed);
        }
        return result;
    }

    @Override
    public CtmJSONObject singleSettle(Long id) throws Exception {
        String key = ICmpConstant.CURRENCYEXCHANGELIST_SINGLESETTLE + id;
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key);
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102490"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180717", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
        }
        CtmJSONObject result = new CtmJSONObject();
        result.put("dealSucceed", false);
        try {
            CurrencyExchange currencyExchange = MetaDaoHelper.findById(CurrencyExchange.ENTITY_NAME, id);
            if (null == currencyExchange) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102491"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180718", "单据不存在 id:") /* "单据不存在 id:" */ + id);
            }
            if (currencyExchange.getAuditstatus() != null && currencyExchange.getAuditstatus().getValue() == AuditStatus.Incomplete.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102484"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189FAC8005C0000C", "该单据未审批，不能进行交割！") /* "该单据未审批，不能进行交割！" */);
            }
            if (currencyExchange.get("settlemode") == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102485"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806FC", "结算方式为空！") /* "结算方式为空！" */);
            }

            if (currencyExchange.getSettlestatus() != null && currencyExchange.getSettlestatus().getValue() == SettleStatus.alreadySettled.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102486"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806F8", "单据已交割完成不能重复交割！") /* "单据已交割完成不能重复交割！" */);
            }
            //交割新增,直联交割不能结算 当前单据为直联单据，手工结算失败。
            if (currencyExchange.getDeliveryType() != null && currencyExchange.getDeliveryType() == DeliveryType.DirectDelivery.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102487"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806FA", "当前单据为直联单据，手工交割失败。") /* "当前单据为直联单据，手工交割失败。" */);
            }

            if (currencyExchange.getBilltype() == null) {
                currencyExchange.set("billtype", EventType.CurrencyExchangeBill.getValue());
            }

            if (BillInfoUtils.getBusinessDate() != null) {
                currencyExchange.setSettledate(BillInfoUtils.getBusinessDate());
            } else {
                currencyExchange.setSettledate(AppContext.getLoginDate());
            }
//            if (currencyExchange.getSettledate().compareTo(currencyExchange.getAuditDate()) < 0) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102488"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180705","单据结算日期小于审核日期，不能交割") /* "单据结算日期小于审核日期，不能交割" */);
//            }
            //校验结算日期是否已日结
            Date maxSettleDate = settlementService.getMaxSettleDate(currencyExchange.getAccentity());
            if (SettleCheckUtil.checkDailySettlement(maxSettleDate, false)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102489"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180709", "当前日结日期为[%s]，交割业务日期不能小于等于日结日期！") /* "当前日结日期为[%s]，交割业务日期不能小于等于日结日期！" */, maxSettleDate));
            }
            currencyExchange.set("_entityName", CurrencyExchange.ENTITY_NAME);
            CtmJSONObject generateResult = cmpVoucherService.generateVoucherWithResult(currencyExchange);
            if (!generateResult.getBoolean("dealSucceed")) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101118"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418070C", "发送会计平台失败：") /* "发送会计平台失败：" */ + generateResult.get("message"));
            }
            if (currencyExchange.getBilltype() == null) {
                currencyExchange.set("billtype", EventType.CurrencyExchangeBill.getValue());
            }
            //手工交割要修改为手工交割成功
            currencyExchange.setSettlestatus(DeliveryStatus.completeDelivery);
            boolean implement = cmpBudgetCurrencyExchangeManagerService.implement(currencyExchange);
            if (implement) {
                currencyExchange.setIsOccupyBudget(OccupyBudget.ActualSuccess.getValue());
            }
            journalService.updateJournal(currencyExchange);
            EntityTool.setUpdateStatus(currencyExchange);
            MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, currencyExchange);
            if (currencyExchange.getBilltype() != null && currencyExchange.getBilltype() == EventType.CurrencyExchangeApply.getValue()) {
                // 外币兑换申请，事项类型的单据，需更新外币申请单据状态
                currencyApplyService.updateDeliveryStatus(currencyExchange.getCurrencyapplyid(), DeliveryStatus.completeDelivery.getValue(), new Date());
            }
            ctmcmpBusinessLogService.saveBusinessLog(currencyExchange, currencyExchange.getCode(), "", IServicecodeConstant.CURRENCYEXCHANGE, IMsgConstant.CURRENCY_EXCHANGE, IMsgConstant.SETTLE);
            result.put(ICmpConstant.MSG, ResultMessage.success());
            result.put("dealSucceed", true);
            result.put("data", currencyExchange);
        } catch (Exception e) {
            result.put(ICmpConstant.MSG, e.getMessage());
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        return result;
    }

    @Override
    public CtmJSONObject singleUnSettle(Long id) throws Exception {
        String key = ICmpConstant.CURRENCYEXCHANGELIST_SINGLESETTLE + id;
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(key);
        if (null == ymsLock) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102490"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180717", "该数据正在处理，请稍后重试！") /* "该数据正在处理，请稍后重试！" */);
        }
        CtmJSONObject result = new CtmJSONObject();
        result.put("dealSucceed", false);
        try {
            CurrencyExchange currencyExchange = MetaDaoHelper.findById(CurrencyExchange.ENTITY_NAME, id);
            if (null == currencyExchange) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102491"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180718", "单据不存在 id:") /* "单据不存在 id:" */ + id);
            }

            if (currencyExchange.getSettlestatus() != null && currencyExchange.getSettlestatus().getValue() == SettleStatus.noSettlement.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102492"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180703", "该单据未交割完成，不能进行取消交割！") /* "该单据未交割完成，不能进行取消交割！" */);
            }
            //交割新增,直联交割不能结算 当前单据为直联单据，手工结算失败。
            if (currencyExchange.getDeliveryType() != null && currencyExchange.getDeliveryType() == DeliveryType.DirectDelivery.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102493"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418070B", "当前单据为直联单据，取消手工交割失败。") /* "当前单据为直联单据，取消手工交割失败。" */);
            }
            if (currencyExchange.getAuditstatus() != null && currencyExchange.getAuditstatus().getValue() == AuditStatus.Incomplete.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102494"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418070F", "该单据未审批，不能进行取消手工交割！") /* "该单据未审批，不能进行取消手工交割！" */);
            }
            //已日结后不能修改或删除期初数据
            Date maxSettleDate = maxSettleDate = settlementService.getMaxSettleDate(currencyExchange.getAccentity());
            if (SettleCheckUtil.checkDailySettlementBeforeUnSettle(maxSettleDate, currencyExchange.getSettledate())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102495"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180716", "该单据已日结，不能取消交割！") /* "该单据已日结，不能取消交割！" */);
            }
            //已日结后不能修改或删除期初数据
            Boolean checkFlag = journalService.checkJournal(currencyExchange.getId());
            if (checkFlag) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102496"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418071A", "该单据已勾对，不能取消交割！") /* "该单据已勾对，不能取消交割！" */);
            }
            CtmJSONObject CtmJSONObject = new CtmJSONObject();
            CtmJSONObject.put("id", currencyExchange.getId());
            CtmJSONObject.put("billnum", "cmp_currencyexchange");
            boolean checked = cmpVoucherService.isChecked(CtmJSONObject);
            if (checked) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102497"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418071D", "该单据凭证已勾对，不能取消交割！") /* "该单据凭证已勾对，不能取消交割！" */);
            }
            if (currencyExchange.getBilltype() == null) {
                currencyExchange.set("billtype", EventType.CurrencyExchangeBill.getValue());
            }
            currencyExchange.set("_entityName", CurrencyExchange.ENTITY_NAME);
            CtmJSONObject deleteResult = cmpVoucherService.deleteVoucherWithResult(currencyExchange);
            if (!deleteResult.getBoolean("dealSucceed")) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102498"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180722", "删除凭证失败：") /* "删除凭证失败：" */ + deleteResult.get("message"));
            }

            currencyExchange.setSettlestatus(DeliveryStatus.todoDelivery);
            CurrencyExchange oldBill = findById(CurrencyExchange.ENTITY_NAME, currencyExchange.getId());
            Short occupyBudget = budgetAfterUnSettle(oldBill);
            if (occupyBudget != null) {
                currencyExchange.setIsOccupyBudget(occupyBudget);
            }
            journalService.updateJournal(currencyExchange);
            //取消结算，结算日期（交割日期置空）
            currencyExchange.setSettledate(null);
            currencyExchange.setVoucherNo(null);
            currencyExchange.setVoucherPeriod(null);
            currencyExchange.setVoucherId(null);
            EntityTool.setUpdateStatus(currencyExchange);
            MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, currencyExchange);
            try {
                ctmcmpBusinessLogService.saveBusinessLog(currencyExchange, currencyExchange.getCode(), "", IServicecodeConstant.CURRENCYEXCHANGE, IMsgConstant.CURRENCY_EXCHANGE, IMsgConstant.UNSETTLE);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            if (currencyExchange.getBilltype() != null && currencyExchange.getBilltype() == EventType.CurrencyExchangeApply.getValue()) {
                // 外币兑换申请，事项类型的单据，需更新外币申请单据状态
                currencyApplyService.updateDeliveryStatus(currencyExchange.getCurrencyapplyid(), DeliveryStatus.todoDelivery.getValue(), new Date());
            }
            result.put("data", currencyExchange);
            result.put("dealSucceed", true);
        } catch (Exception e) {
            result.put(ICmpConstant.MSG, e.getMessage());
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
        return result;
    }

    @Override
    public CtmJSONObject unSettle(List<CurrencyExchange> currencyExchangeList) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        List<String> messages = new ArrayList<>();
        //最大日结日期
        Map<String, Date> maxSettleDateMaps = new HashMap<String, Date>();
        if (currencyExchangeList == null || currencyExchangeList.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102483"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418072E", "请选择单据！") /* "请选择单据！" */);
        }
        Long[] ids = new Long[currencyExchangeList.size()];
        for (int i = 0; i < currencyExchangeList.size(); i++) {
            ids[i] = currencyExchangeList.get(i).getId();
        }
        CtmJSONObject failed = new CtmJSONObject();
        List<CurrencyExchange> currencyExchangeDBList = this.queryByIds(ids);
        int i = 0;
        for (CurrencyExchange currencyExchange : currencyExchangeDBList) {
            if (currencyExchange.getSettlestatus() != null && currencyExchange.getSettlestatus().getValue() == SettleStatus.noSettlement.getValue()) {
                failed.put(currencyExchange.getId().toString(), currencyExchange.getId().toString());
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180701", "单据[%s]未交割完成，不能进行取消交割！") /* "单据[%s]未交割完成，不能进行取消交割！" */, currencyExchange.getCode()));
                if (currencyExchangeDBList.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102492"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180703", "该单据未交割完成，不能进行取消交割！") /* "该单据未交割完成，不能进行取消交割！" */);
                }
                i++;
                continue;
            }
            //直联交割不能手工取消结算
            if (currencyExchange.getDeliveryType() != null && currencyExchange.getDeliveryType() == DeliveryType.DirectDelivery.getValue()) {
                failed.put(currencyExchange.getId().toString(), currencyExchange.getId().toString());
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418070A", "单据[%s]为直联单据，取消手工交割失败。") /* "单据[%s]为直联单据，取消手工交割失败。" */, currencyExchange.getCode()));
                if (currencyExchangeDBList.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102493"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418070B", "当前单据为直联单据，取消手工交割失败。") /* "当前单据为直联单据，取消手工交割失败。" */);
                }
                i++;
                continue;
            }

            if (currencyExchange.getAuditstatus() != null && currencyExchange.getAuditstatus().getValue() == AuditStatus.Incomplete.getValue()) {
                failed.put(currencyExchange.getId().toString(), currencyExchange.getId().toString());
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418070E", "单据[%s]未审批，不能进行取消手工交割！") /* "单据[%s]未审批，不能进行取消手工交割！" */, currencyExchange.getCode()));
                if (currencyExchangeDBList.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102494"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418070F", "该单据未审批，不能进行取消手工交割！") /* "该单据未审批，不能进行取消手工交割！" */);
                }
                i++;
                continue;
            }
            //已日结后不能修改或删除期初数据
            Date maxSettleDate = null;
            if (maxSettleDateMaps.containsKey(currencyExchange.getAccentity())) {
                maxSettleDate = maxSettleDateMaps.get(currencyExchange.getAccentity());
            } else {
                maxSettleDate = settlementService.getMaxSettleDate(currencyExchange.getAccentity());
                maxSettleDateMaps.put(currencyExchange.getAccentity(), maxSettleDate);
            }
            if (SettleCheckUtil.checkDailySettlementBeforeUnSettle(maxSettleDate, currencyExchange.getSettledate())) {
                failed.put(currencyExchange.getId().toString(), currencyExchange.getId().toString());
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180714", "单据[%s]已日结，不能取消交割！") /* "单据[%s]已日结，不能取消交割！" */, currencyExchange.getCode()));
                if (currencyExchangeDBList.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102495"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180716", "该单据已日结，不能取消交割！") /* "该单据已日结，不能取消交割！" */);
                }
                i++;
                continue;
            }
            //已日结后不能修改或删除期初数据
            Boolean checkFlag = journalService.checkJournal(currencyExchange.getId());
            if (checkFlag) {
                failed.put(currencyExchange.getId().toString(), currencyExchange.getId().toString());
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180719", "单据[%s]已勾对，不能取消交割！") /* "单据[%s]已勾对，不能取消交割！" */, currencyExchange.getCode()));
                if (currencyExchangeDBList.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102496"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418071A", "该单据已勾对，不能取消交割！") /* "该单据已勾对，不能取消交割！" */);
                }
                i++;
                continue;
            }
            CtmJSONObject CtmJSONObject = new CtmJSONObject();
            CtmJSONObject.put("id", currencyExchange.getId());
            CtmJSONObject.put("billnum", "cmp_currencyexchange");
            boolean checked = cmpVoucherService.isChecked(CtmJSONObject);
            if (checked) {
                failed.put(currencyExchange.getId().toString(), currencyExchange.getId().toString());
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418071C", "单据[%s]凭证已勾对，不能取消交割！") /* "单据[%s]凭证已勾对，不能取消交割！" */, currencyExchange.getCode()));
                if (currencyExchangeDBList.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102497"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418071D", "该单据凭证已勾对，不能取消交割！") /* "该单据凭证已勾对，不能取消交割！" */);
                }
                i++;
                continue;
            }
            if (currencyExchange.getBilltype() == null) {
                currencyExchange.set("billtype", EventType.CurrencyExchangeBill.getValue());
            }
//            currencyExchange.setSettleuser(AppContext.getCurrentUser().getId().toString());
//            currencyExchange.setSettledate(AppContext.getLoginDate());
            currencyExchange.set("_entityName", CurrencyExchange.ENTITY_NAME);
            CtmJSONObject deleteResult = cmpVoucherService.deleteVoucherWithResult(currencyExchange);
            if (!deleteResult.getBoolean("dealSucceed")) {
                failed.put(currencyExchange.getId().toString(), currencyExchange.getId().toString());
                messages.add(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180721", "单据[%s]删除凭证失败，不能取消交割！") /* "单据[%s]删除凭证失败，不能取消交割！" */, currencyExchange.getCode()));
                if (currencyExchangeDBList.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102498"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180722", "删除凭证失败：") /* "删除凭证失败：" */ + deleteResult.get("message"));
                }
                i++;
                continue;
            }
            currencyExchange.setSettlestatus(DeliveryStatus.todoDelivery);
            CurrencyExchange oldBill = findById(CurrencyExchange.ENTITY_NAME, currencyExchange.getId());
            Short occupyBudget = budgetAfterUnSettle(oldBill);
            if (occupyBudget != null) {
                currencyExchange.setIsOccupyBudget(occupyBudget);
            }
            journalService.updateJournal(currencyExchange);
            //取消结算，结算日期（交割日期置空）
            currencyExchange.setSettledate(null);
            currencyExchange.setVoucherNo(null);
            currencyExchange.setVoucherId(null);
            currencyExchange.setVoucherPeriod(null);
            EntityTool.setUpdateStatus(currencyExchange);
            MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, currencyExchange);
            if (currencyExchange.getBilltype() != null && currencyExchange.getBilltype() == EventType.CurrencyExchangeApply.getValue()) {
                // 外币兑换申请，事项类型的单据，需更新外币申请单据状态
                currencyApplyService.updateDeliveryStatus(currencyExchange.getCurrencyapplyid(), DeliveryStatus.todoDelivery.getValue(), new Date());
            }
            ctmcmpBusinessLogService.saveBusinessLog(currencyExchange, currencyExchange.getCode(), "", IServicecodeConstant.CURRENCYEXCHANGE, IMsgConstant.CURRENCY_EXCHANGE, IMsgConstant.UNSETTLE);
        }
        String message = null;
        if (currencyExchangeDBList.size() == 1) {
            message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180731", "取消结算成功!") /* "取消结算成功!" */;
        } else {
            message = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1818746205B0000B", "共：%s张单据；%s张手工交割成功；%s张手工交割失败！") /* "共：%s张单据；%s张手工交割成功；%s张手工交割失败！" */, currencyExchangeDBList.size(), (currencyExchangeDBList.size() - i), i);
//            message = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180728","共：") /* "共：" */ + currencyExchangeDBList.size() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180727","张单据；") /* "张单据；" */ + (currencyExchangeDBList.size() - i) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180733","张取消交割成功；") /* "张取消交割成功；" */ + i + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180734","张取消交割失败！") /* "张取消交割失败！" */;
        }
        result.put("msg", message);
        result.put("msgs", messages);
        result.put("messages", messages);
        result.put("count", currencyExchangeDBList.size());
        result.put("sucessCount", currencyExchangeDBList.size() - i);
        result.put("failCount", i);
        if (failed.size() > 0) {
            result.put("failed", failed);
        }
        return result;
    }

    @Override
    public CtmJSONObject audit(List<CurrencyExchange> currencyExchangeList) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        List<String> messages = new ArrayList<>();
        if (currencyExchangeList == null || currencyExchangeList.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102499"), MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.SELECT_BILL_NOTICE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005AE", "请选择单据！") /* "请选择单据！" */));
        }
        Long[] ids = new Long[currencyExchangeList.size()];
        for (int i = 0; i < currencyExchangeList.size(); i++) {
            Long id = currencyExchangeList.get(i).getId();
            ids[i] = id;
            checkPubTs(currencyExchangeList.get(i).getPubts(), id);
        }
        List<CurrencyExchange> currencyExchangeDBList = this.queryByIds(ids);
        int i = 0;
        CtmJSONObject failed = new CtmJSONObject();
        Date date = BillInfoUtils.getBusinessDate();
        for (CurrencyExchange currencyExchange : currencyExchangeDBList) {
            if (currencyExchange.getSettlestatus() != null && currencyExchange.getSettlestatus().getValue() == 2) {
                failed.put(currencyExchange.getId().toString(), currencyExchange.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806FD", "单据【") /* "单据【" */ + currencyExchange.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180708", "】已结算，不能进行审批！") /* "】已结算，不能进行审批！" */);
                if (currencyExchangeDBList.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102500"), MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.AUDIT_SETTLED_NOTICE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005AD", "该单据已结算，不能进行审批！") /* "该单据已结算，不能进行审批！" */));
                }
                i++;
                continue;
            }
            if (currencyExchange.getAuditstatus() != null && currencyExchange.getAuditstatus().getValue() == AuditStatus.Complete.getValue()) {
                failed.put(currencyExchange.getId().toString(), currencyExchange.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806FD", "单据【") /* "单据【" */ + currencyExchange.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418070D", "】已审批，不能进行重复审批！") /* "】已审批，不能进行重复审批！" */);
                if (currencyExchangeDBList.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102501"), MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.AUDITED_REPEAT_NOTICE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005AF", "该单据已审批，不能进行重复审批！") /* "该单据已审批，不能进行重复审批！" */));
                }
                i++;
                continue;
            }
            if (currencyExchange.getSrcitem() != null && currencyExchange.getSrcitem().getValue() != EventSource.Cmpchase.getValue()) {
                failed.put(currencyExchange.getId().toString(), currencyExchange.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806FD", "单据【") /* "单据【" */ + currencyExchange.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180710", "】不是现金自制单据，不能进行审批！") /* "】不是现金自制单据，不能进行审批！" */);
                if (currencyExchangeDBList.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102502"), MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.AUDIT_SCRIME_NOTICE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005B5", "该单据不是现金自制单据，不能进行审批！") /* "该单据不是现金自制单据，不能进行审批！" */));
                }
                i++;
                continue;
            }
            if (currencyExchange.getVouchdate() != null && date.compareTo(currencyExchange.getVouchdate()) < 0) {
                failed.put(currencyExchange.getId().toString(), currencyExchange.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806FD", "单据【") /* "单据【" */ + currencyExchange.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180712", "】的审核日期小于单据日期不能审批！") /* "】的审核日期小于单据日期不能审批！" */);
                if (currencyExchangeDBList.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102503"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180715", "审核日期小于单据日期不能审批") /* "审核日期小于单据日期不能审批" */);
                }
                i++;
                //messages.add("单据【" + rowData.get("code") + com.yonyou.iuap.ucf.common.i18n.MessageUtils.getMessage("P_YS_FI_CM_0000153355") /* "】的审核日期小于单据日期不能审批" */);
                //failedCount++;
                continue;
            }
            currencyExchange.setAuditstatus(AuditStatus.Complete);
            currencyExchange.setAuditorId(AppContext.getCurrentUser().getId());
            currencyExchange.setAuditor(AppContext.getCurrentUser().getName());

            currencyExchange.setAuditDate(BillInfoUtils.getBusinessDate());
            currencyExchange.setAuditTime(new Date());

        }
        EntityTool.setUpdateStatus(currencyExchangeDBList);
        MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, currencyExchangeDBList);
        String message = null;
        if (currencyExchangeDBList.size() == 1) {
            message = MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.AUDITED_SUCCESS_NOTICE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005BF", "审批成功!") /* "审批成功!" */);
        } else {
            message = MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.TOTAL, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005B1", "共：") /* "共：" */) + currencyExchangeDBList.size() + MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.TOTAL_BILL, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005B2", "张单据；") /* "张单据；" */) + (currencyExchangeDBList.size() - i) + MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.AUDITED_TOTLASUCCESS_NOTICE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005C0", "张审批通过；") /* "张审批通过；" */) + i + MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.AUDITED_TOTLAFIEDL_NOTICE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005C1", "张审批未通过！") /* "张审批未通过！" */);
        }
        result.put(ICmpConstant.MSG, message);
        result.put("msgs", messages);
        result.put("messages", messages);
        result.put("count", currencyExchangeDBList.size());
        result.put("sucessCount", currencyExchangeDBList.size() - i);
        result.put("failCount", i);
        if (failed.size() > 0) {
            result.put(ICmpConstant.FAILED, failed);
        }
        return result;
    }

    @Override
    public CtmJSONObject unAudit(List<CurrencyExchange> currencyExchangeList) throws Exception {
        CtmJSONObject result = new CtmJSONObject();
        List<String> messages = new ArrayList<>();
        if (currencyExchangeList == null || currencyExchangeList.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102504"), MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.SELECT_BILL_NOTICE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005AE", "请选择单据！") /* "请选择单据！" */));
        }
        Long[] ids = new Long[currencyExchangeList.size()];
        for (int i = 0; i < currencyExchangeList.size(); i++) {
            ids[i] = currencyExchangeList.get(i).getId();
        }
        List<CurrencyExchange> currencyExchangeDBList = this.queryByIds(ids);
        int i = 0;
        CtmJSONObject failed = new CtmJSONObject();
        for (CurrencyExchange currencyExchange : currencyExchangeDBList) {
            //交割相关，更改为已结算的不能弃审
            if (currencyExchange.getSettlestatus() != null && (currencyExchange.getSettlestatus().getValue() == 2
                    || currencyExchange.getSettlestatus().getValue() == 3 || currencyExchange.getSettlestatus().getValue() == 4 || currencyExchange.getSettlestatus().getValue() == 5)) {
                failed.put(currencyExchange.getId().toString(), currencyExchange.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806FD", "单据【") /* "单据【" */ + currencyExchange.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418072B", "】已结算，不能进行取消审批！") /* "】已结算，不能进行取消审批！" */);
                if (currencyExchangeDBList.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102505"), MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.UNAUDIT_SETTLED_NOTICE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005B6", "该单据已结算，不能进行取消审批！") /* "该单据已结算，不能进行取消审批！" */));
                }
                i++;
                continue;
            }
            if (currencyExchange.getAuditstatus() != null && currencyExchange.getAuditstatus().getValue() == AuditStatus.Incomplete.getValue()) {
                failed.put(currencyExchange.getId().toString(), currencyExchange.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806FD", "单据【") /* "单据【" */ + currencyExchange.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180732", "】未审批，不能进行取消审批！") /* "】未审批，不能进行取消审批！" */);
                if (currencyExchangeDBList.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102506"), MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.UNAUDITED_NOTICE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005B8", "该单据未审批，不能进行取消审批！") /* "该单据未审批，不能进行取消审批！" */));
                }
                i++;
                continue;
            }
            if (currencyExchange.getSrcitem() != null && currencyExchange.getSrcitem().getValue() != EventSource.Cmpchase.getValue()) {
                failed.put(currencyExchange.getId().toString(), currencyExchange.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806FD", "单据【") /* "单据【" */ + currencyExchange.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180735", "】不是现金自制单据，不能进行取消审批！") /* "】不是现金自制单据，不能进行取消审批！" */);
                if (currencyExchangeDBList.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102507"), MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.UNAUDIT_SCRIME_NOTICE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005BC", "该单据不是现金自制单据，不能进行取消审批！") /* "该单据不是现金自制单据，不能进行取消审批！" */));
                }
                i++;
                continue;
            }
            //已日结后不能修改或删除期初数据
            QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.SELECT_ONE_PARAM);
            querySchema.addCondition(QueryConditionGroup.and(QueryCondition.name(SETTLE_FLAG).eq(1), QueryCondition.name(SETTLEMENT_DATE).eq(currencyExchange.get(ICmpConstant.VOUCHDATE))
                    , QueryCondition.name(IBussinessConstant.ACCENTITY).eq(currencyExchange.get(IBussinessConstant.ACCENTITY))));
            List<Settlement> settlementList = MetaDaoHelper.query(Settlement.ENTITY_NAME, querySchema);
            if (ValueUtils.isNotEmpty(settlementList) && settlementList.size() > 0) {
                failed.put(currencyExchange.getId().toString(), currencyExchange.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806FD", "单据【") /* "单据【" */ + currencyExchange.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806FE", "】已日结，不能取消审批！") /* "】已日结，不能取消审批！" */);
                if (currencyExchangeList.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102508"), MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.UNAUDIT_DAYSETTLED_NOTICE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005A9", "该单据已日结，不能取消审批！") /* "该单据已日结，不能取消审批！" */));
                }
                i++;
                continue;
            }

            if (journalService.checkJournal(currencyExchange.getId())) {
                failed.put(currencyExchange.getId().toString(), currencyExchange.getId().toString());
                messages.add(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806FD", "单据【") /* "单据【" */ + currencyExchange.getCode() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180706", "】已勾对，不能取消审批！") /* "】已勾对，不能取消审批！" */);
                if (currencyExchangeDBList.size() == 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102509"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180707", "该单据已勾对，不能取消审批！") /* "该单据已勾对，不能取消审批！" */);
                }
                i++;
                continue;
            }

            currencyExchange.setAuditstatus(AuditStatus.Incomplete);
            currencyExchange.setAuditorId(null);
            currencyExchange.setAuditor(null);
            currencyExchange.setAuditTime(null);
            currencyExchange.setAuditDate(null);
        }
        EntityTool.setUpdateStatus(currencyExchangeDBList);
        MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, currencyExchangeDBList);
        String message = null;
        if (currencyExchangeDBList.size() == 1) {
            message = MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.UNAUDITED_SUCCESS_NOTICE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005B0", "取消审批成功!") /* "取消审批成功!" */);
        } else {
            message = MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.TOTAL, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005B1", "共：") /* "共：" */) + currencyExchangeDBList.size() + MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.TOTAL_BILL, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005B2", "张单据；") /* "张单据；" */) + (currencyExchangeDBList.size() - i) + MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.UNAUDITED_TOTLASUCCESS_NOTICE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005B3", "张取消审批成功；") /* "张取消审批成功；" */) + i + MessageUtils.getMessageWithDefault(ICurrencyExchangeNoticeMsgConstant.UNAUDITED_TOTLAFIEDL_NOTICE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005B4", "张取消审批失败！") /* "张取消审批失败！" */);
        }
        result.put("msgs", messages);
        result.put("messages", messages);
        result.put("count", currencyExchangeDBList.size());
        result.put("sucessCount", currencyExchangeDBList.size() - i);
        result.put("failCount", i);
        result.put(ICmpConstant.MSG, message);
        if (failed.size() > 0) {
            result.put(ICmpConstant.FAILED, failed);
        }
        return result;
    }

    /**
     * 获取用户自定义类型
     *
     * @return
     */
    @Override
    public String getRateType(CtmJSONObject param) throws Exception {
        CtmJSONObject data = param.getJSONObject("data");
        /*Object code = "02";
        if (ValueUtils.isNotEmptyObj(data)) {
            code = data.get("code") != null ? data.get("code") : "02";
        }
        BillContext billContext = new BillContext();
        billContext.setFullname("bd.exchangeRate.ExchangeRateTypeVO");
        billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        conditionGroup.appendCondition(QueryCondition.name("code").eq(code));
        //conditionGroup.appendCondition(QueryCondition.name("name").eq("用户自定义汇率"));
        schema.addCondition(conditionGroup);
        List<Map<String, Object>> query = MetaDaoHelper.query(billContext, schema);*/
        Map<String, Object> rateType = Maps.newHashMap();
        ExchangeRateTypeVO exchangeRateType;
        if (data.get("code") != null) {
            if("02".equals(data.get("code").toString())){
                exchangeRateType = CmpExchangeRateUtils.getUserDefineExchangeRateType();
            } else {
                exchangeRateType = AppContext.getBean(CmCommonService.class).getExchangeRateType(data.get("code").toString());
            }
        } else {
            if(data.get("accentity") != null){
                exchangeRateType = CmpExchangeRateUtils.getNewExchangeRateType(data.get("accentity").toString(), true);
            } else {
                exchangeRateType = new ExchangeRateTypeVO();
            }
        }
        if (exchangeRateType != null) {
            rateType.put("id", exchangeRateType.getId());
            rateType.put("name", exchangeRateType.getName());
            rateType.put("digit", exchangeRateType.getDigit());
        }
        return ResultMessage.data(rateType);
    }


    @Override
    public String getRateTypeByFundBill(CtmJSONObject param) throws Exception {
        CtmJSONObject data = param.getJSONObject("data");
        Object code = data.get("code");
        Map<String, Object> rateType = Maps.newHashMap();
        if ("01".equals(code)) {
            Map<String, Object> defaultExchangeRateType = cmCommonService.getDefaultExchangeRateType(data.getString("accentity"));
            if (defaultExchangeRateType != null && defaultExchangeRateType.get("id") != null) {
                rateType.put("id", defaultExchangeRateType.get("id"));
                rateType.put("name", defaultExchangeRateType.get("name"));
                rateType.put("digit", defaultExchangeRateType.get("digit"));
            }
        } else {
            BillContext billContext = new BillContext();
            billContext.setFullname("bd.exchangeRate.ExchangeRateTypeVO");
            billContext.setDomain(IDomainConstant.MDD_DOMAIN_UCFBASEDOC);
            QuerySchema schema = QuerySchema.create().addSelect("id, name, digit");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name("code").eq(code));
            schema.addCondition(conditionGroup);
            List<Map<String, Object>> query = MetaDaoHelper.query(billContext, schema);
            if (query != null && !query.isEmpty()) {
                rateType.put("id", query.get(0).get("id"));
                rateType.put("name", query.get(0).get("name"));
                rateType.put("digit", query.get(0).get("digit"));
            }
        }
        return ResultMessage.data(rateType);
    }

    /*
     *@Author
     *@Description 校验时间戳    外币兑换审核
     *@Date 2020/7/4 10:20
     *@Param [rows]
     *@Return void
     **/
    private void checkPubTs(Date puts, Long id) throws Exception {
        CurrencyExchange currencyExchange = findById(CurrencyExchange.ENTITY_NAME, id);
        if (puts.compareTo(currencyExchange.getPubts()) != 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102510"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180723", "数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
        }
    }

    @Override
    public CtmJSONObject currencyExchangeSubmit(CtmJSONObject param) throws Exception {
        Long currencyExchangeId = Long.valueOf(param.get("id").toString());
        // 加redis锁，防止短时间内重复提交
        YmsLock ymsLock = JedisLockUtils.lockBillWithOutTrace(currencyExchangeId.toString());
        try {
            if (null == ymsLock) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102511"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418072A", "该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
            }
            CurrencyExchange currencyExchange = MetaDaoHelper.findById(CurrencyExchange.ENTITY_NAME, currencyExchangeId);
            if (currencyExchange.getAuditstatus() != AuditStatus.Complete) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102512"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418072D", "当前单据未审批通过，提交指令失败") /* "当前单据未审批通过，提交指令失败" */);
            }
            if (currencyExchange.getSettlestatus() != DeliveryStatus.todoDelivery) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102513"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418072F", "当前单据交割状态非待处理状态，提交指令失败") /* "当前单据交割状态非待处理状态，提交指令失败" */);
            }
            if (currencyExchange.getDeliveryType() != DeliveryType.DirectDelivery.getValue()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102514"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180730", "当前单据为非直联单据，提交指令失败") /* "当前单据为非直联单据，提交指令失败" */);
            }
            //查询银企连账号 看是否为直连
            if (currencyExchange.getPurchasebankaccount() != null && currencyExchange.getSellbankaccount() != null) {
                List<String> ids = new ArrayList<>();
                ids.add(currencyExchange.getPurchasebankaccount());
                ids.add(currencyExchange.getSellbankaccount());
                CommonRequestDataVo commonQueryData = new CommonRequestDataVo();
                commonQueryData.setIds(ids);
                Map<String, Boolean> map = bankAccountSettingService.getOpenFlagReMap(commonQueryData);
                for (String key : map.keySet()) {
                    if (!map.get(key)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102515"), String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187F435E05B00007", "银行账户【%s】没有开通银企联服务，指令提交失败，请前往【银企联账号】检查银行账户直联权限。") /* "银行账户【%s】没有开通银企联服务，指令提交失败，请前往【银企联账号】检查银行账户直联权限。" */, key));
                    }
                }
            }

            param.put("tran_seq_no", ymsOidGenerator.nextId());
            // 业务批次号，
            if (currencyExchange.getBatchno() == null || StringUtils.isEmpty(currencyExchange.getBatchno())) {
                String batchNo = ymsOidGenerator.nextStrId();
                param.put("batch_no", batchNo);
                currencyExchange.setBatchno(batchNo);
            } else {
                param.put("batch_no", currencyExchange.getBatchno());
            }
            Map sellAcctNoMap = QueryBaseDocUtils.queryEnterpriseBankAccountById(currencyExchange.getSellbankaccount());
            param.put("sell_acct_no", sellAcctNoMap.get("account"));
            param.put("sell_acct_no_name", sellAcctNoMap.get("name"));
            Map buyAcctNoMap = QueryBaseDocUtils.queryEnterpriseBankAccountById(currencyExchange.getPurchasebankaccount());
            param.put("buy_acct_no", buyAcctNoMap.get("account"));
            param.put("buy_acct_no_name", buyAcctNoMap.get("name"));
            CurrencyTenantDTO sellCUrrencyDTO = baseRefRpcService.queryCurrencyById(currencyExchange.getSellCurrency());
            param.put("sell_curr", sellCUrrencyDTO.getCode());
            CurrencyTenantDTO buyCUrrencyDTO = baseRefRpcService.queryCurrencyById(currencyExchange.getPurchaseCurrency());
            param.put("buy_curr", buyCUrrencyDTO.getCode());
            if (currencyExchange.getDepositAccountNo() != null) {
                Map depositAcctNoMap = QueryBaseDocUtils.queryEnterpriseBankAccountById(currencyExchange.getDepositAccountNo());
                param.put("deposit_acct_no", depositAcctNoMap.get("account"));
            } else {
                param.put("deposit_acct_no", null);
            }
            if (currencyExchange.getTransactionCode() != null) {
                BizObject exchangeSettlementTradeCode = MetaDaoHelper.findById(ExchangeSettlementTradeCode.ENTITY_NAME, currencyExchange.getTransactionCode());
                param.put("trade_code", exchangeSettlementTradeCode.get("trade_code"));
                param.put("st_code", exchangeSettlementTradeCode.get("statisticscode"));
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102516"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180702", "交易编码字段必输！") /* "交易编码字段必输！" */);
            }
            //202404来源代码逻辑调整 结汇，来源编码=来源代码；购汇来源编码=用途代码
            if (Bsflag.Buy == currencyExchange.getFlag()) {
                // 用途代码
                if (currencyExchange.getPurposecode() != null) {
                    ExchangeSourceCode exchangeSourceCode = MetaDaoHelper.findById(ExchangeSourceCode.ENTITY_NAME, currencyExchange.getPurposecode());
                    param.put("source_code", exchangeSourceCode.getCoding());
                }
            } else {
                // 来源代码
                if (currencyExchange.getSourcecode() != null) {
                    ExchangeSourceCode exchangeSourceCode = MetaDaoHelper.findById(ExchangeSourceCode.ENTITY_NAME, currencyExchange.getSourcecode());
                    param.put("source_code", exchangeSourceCode.getCoding());
                }
            }
            if (currencyExchange.getSettlePurposeCode() != null) {
                BizObject exchangeSettlementPurpose = MetaDaoHelper.findById(ExchangeSettlementPurpose.ENTITY_NAME, currencyExchange.getSettlePurposeCode());
                param.put("for_ex_useof", exchangeSettlementPurpose.get("purposecode"));
            } else {
                param.put("for_ex_useof", null);
                param.put("for_ex_useof_det", null);
            }
            String customNo = bankAccountSettingService.getCustomNoAndCheckByBankAccountId(currencyExchange.getPurchasebankaccount(), param.get("customNo"));
            param.put("customNo", customNo);
            //银企联附件上传
            CtmJSONArray fileInfoList = null;
            try {
                fileInfoList = uploadDeliveryFile(param, currencyExchange);
            } catch (Exception e) {
                log.error("附件上传异常：" + e.getMessage());
            }
            if (ObjectUtils.isNotEmpty(fileInfoList)) {
                param.put("fileInfo", fileInfoList);
            }
            //交割提交时给请求流水号赋值新的
            param.put("tran_seq_no", ymsOidGenerator.nextId());
            CtmJSONObject placeOrderMsg = BankEnterpriseAssociation.buildReqDataSSFE1002(param, currencyExchange);
            String placeOrderString = CtmJSONObject.toJSONString(placeOrderMsg);
            String signMsg = bankConnectionAdapterContext.chanPaySignMessage(placeOrderString);
            List<BasicNameValuePair> requestData = new ArrayList<>();
            requestData.add(new BasicNameValuePair("reqData", placeOrderString));
            requestData.add(new BasicNameValuePair("reqSignData", signMsg));
            log.error("===================>外币兑换交割指令提交请求数据" + CtmJSONObject.toJSONString(requestData));
            CtmJSONObject result = new CtmJSONObject();
            CtmJSONObject responseBody = new CtmJSONObject();
            // 修改单据状态为处理中，防止重复提交操作，使用独立事务，不向上传播
            currencyExchangeManager.updateStatus(currencyExchange);
            CurrencyExchange newCurrencyExchange = MetaDaoHelper.findById(CurrencyExchange.ENTITY_NAME,
                    currencyExchangeId);//currencyExchangeId
            newCurrencyExchange.setId(currencyExchangeId);
//            if (HttpsUtils.isWhiteUrl(bankConnectionAdapterContext.getChanPayUri())) {
            if (Bsflag.Exchange == currencyExchange.getFlag()) {
                // 外币兑换，使用 CURRENCY_EXCHANGE_SUBMIT_EACH，SSFE1018接口
                result = HttpsUtils.doHttpsPostNew(CURRENCY_EXCHANGE_SUBMIT_EACH, requestData, bankConnectionAdapterContext.getChanPayUri());
            } else {
                result = HttpsUtils.doHttpsPostNew(CURRENCY_EXCHANGE_SUBMIT, requestData, bankConnectionAdapterContext.getChanPayUri());
            }
//            }
            log.error("===================>外币兑换交割指令提交返回数据" + CtmJSONObject.toJSONString(requestData));
            CtmJSONObject logJsonObject = new CtmJSONObject();
            logJsonObject.put("requestData", requestData);
            logJsonObject.put("responseData", result);
            ctmcmpBusinessLogService.saveBusinessLog(logJsonObject, currencyExchange.getCode() + "_SUBMIT", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00112", "外币兑换交割指令提交") /* "外币兑换交割指令提交" */, "ficmp0040", IMsgConstant.CURRENCY_EXCHANGE, IMsgConstant.CURRENCY_EXCHANGE_SUBMIT);
            if (result.getInteger("code") == 1) {
                CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
                String serviceStatus = responseHead.getString("service_status");
                if (("00").equals(serviceStatus)) {
                    if (result.getJSONObject("data") == null || result.getJSONObject("data").getJSONObject("response_body") == null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102517"), responseHead.get("service_resp_desc").toString());
                    }
                    responseBody = result.getJSONObject("data").getJSONObject("response_body");
                    String deliveryStatus = (String) responseBody.get("delivery_status");
                    if (StringUtils.isEmpty(deliveryStatus)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102518"), responseBody.get("bank_resp_desc").toString());
                    }
                    String respDescStr = org.apache.commons.lang3.StringUtils.truncate(responseHead.get("service_resp_desc").toString(), 1000);
                    newCurrencyExchange.setEnterpriseBackMessg(respDescStr);
                    // 更新交割状态
                    if (responseBody.get("fxtn_ar_id") != null && !StringUtils.isEmpty(responseBody.get("fxtn_ar_id").toString())) {
                        newCurrencyExchange.setContractNo(responseBody.get("fxtn_ar_id").toString());
                    }
                    // 01:录入完成(已删除),02:待交割(需手工交割时返回此状态),03:已交割,04:逾期,05：处理中,06:失败
                    switch (deliveryStatus) {
                        case "02":
                            newCurrencyExchange.setSettlestatus(DeliveryStatus.waitDelivery);
                            newCurrencyExchange.setEntityStatus(EntityStatus.Update);
                            MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, newCurrencyExchange);
                            break;
                        case "03":
                            // 已交割，需生成凭证，更新日记账
                            BigDecimal exchangeRate = new BigDecimal(responseBody.getString("cust_rate"));
                            newCurrencyExchange.setSettlestatus(DeliveryStatus.alreadyDelivery);
                            newCurrencyExchange.setSellamount(new BigDecimal(responseBody.getString("sell_amt")));
                            newCurrencyExchange.setPurchaseamount(new BigDecimal(responseBody.getString("buy_amt")));
                            newCurrencyExchange.setExchangerate(exchangeRate);
                            // 取查回已交割状态时的银企返回交割日期
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                            Date delivery_date = responseBody.getString("delivery_date") != null ?
                                    sdf.parse(responseBody.getString("delivery_date")) : new Date();
                            newCurrencyExchange.setSettledate(delivery_date);
                            alreadyDeliveryUpdate(newCurrencyExchange, exchangeRate);
                            break;
                        case "04":
                            newCurrencyExchange.setSettlestatus(DeliveryStatus.beOverdueDelivery);
                            newCurrencyExchange.setEntityStatus(EntityStatus.Update);
                            CurrencyExchange oldBill = findById(CurrencyExchange.ENTITY_NAME, currencyExchange.getId());
                            boolean releaseBudget = cmpBudgetCurrencyExchangeManagerService.releaseBudget(oldBill);
                            if (releaseBudget) {
                                currencyExchange.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                            }
                            MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, newCurrencyExchange);
                            break;
                        case "05":
                            newCurrencyExchange.setSettlestatus(DeliveryStatus.doingDelivery);
                            newCurrencyExchange.setEntityStatus(EntityStatus.Update);
                            MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, newCurrencyExchange);
                            break;
                        case "06":
                            newCurrencyExchange.setSettlestatus(DeliveryStatus.failDelivery);
                            newCurrencyExchange.setEntityStatus(EntityStatus.Update);
                            MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, newCurrencyExchange);
                            break;
                        default:
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102519"), responseBody.get("bank_resp_desc").toString());
                    }
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102520"), responseHead.get("service_resp_desc").toString());
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102521"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418072C", "即期结售汇交割接口SSFE1003调用异常") /* "即期结售汇交割接口SSFE1003调用异常" */);
            }
            return responseBody;
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }
    }

    @Override
    public CtmJSONObject currencyExchangeDelivery(CtmJSONObject param) throws Exception {
        Long currencyExchangeId = Long.valueOf(param.get("id").toString());
        YmsLock ymsLock = null;
        try {
            // 加redis锁，防止短时间内重复提交
            ymsLock = JedisLockUtils.lockBillWithOutTrace(currencyExchangeId.toString());
            if (null == ymsLock) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102511"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418072A", "该单据已锁定，请稍后重试！") /* "该单据已锁定，请稍后重试！" */);
            }
            CurrencyExchange currencyExchange = MetaDaoHelper.findById(CurrencyExchange.ENTITY_NAME, currencyExchangeId);
            ExchangeDeliveryRequestVO exchangeDeliveryRequestVO = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(param), ExchangeDeliveryRequestVO.class);
            exchangeDeliveryRequestVO.setTran_seq_no(ymsOidGenerator.nextStrId());
            Map sellAcctNoMap = QueryBaseDocUtils.queryEnterpriseBankAccountById(currencyExchange.getSellbankaccount());
            exchangeDeliveryRequestVO.setSell_acct_no(sellAcctNoMap.get("account").toString());
            String customNo = bankAccountSettingService.getCustomNoAndCheckByBankAccountId(param.getString("payBankAccount"), param.get("customNo"));
            exchangeDeliveryRequestVO.setCustomNo(customNo);
            CtmJSONObject placeOrderMsg = BankEnterpriseAssociation.buildReqDataSSFE1003(exchangeDeliveryRequestVO, currencyExchange);
            String placeOrderString = CtmJSONObject.toJSONString(placeOrderMsg);
            String signMsg = bankConnectionAdapterContext.chanPaySignMessage(placeOrderString);
            List<BasicNameValuePair> requestData = new ArrayList<>();
            requestData.add(new BasicNameValuePair("reqData", placeOrderString));
            requestData.add(new BasicNameValuePair("reqSignData", signMsg));
            CtmJSONObject result = new CtmJSONObject();
            CtmJSONObject responseBody = new CtmJSONObject();
//            if (HttpsUtils.isWhiteUrl(bankConnectionAdapterContext.getChanPayUri())) {
            result = HttpsUtils.doHttpsPostNew(CURRENCY_EXCHANGE_DELIVERY, requestData, bankConnectionAdapterContext.getChanPayUri());
//            }
            CtmJSONObject logJsonObject = new CtmJSONObject();
            logJsonObject.put("requestData", requestData);
            logJsonObject.put("responseData", result);
            ctmcmpBusinessLogService.saveBusinessLog(logJsonObject, currencyExchange.getCode() + "_DELIVERY", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0010F", "即期结售汇交割") /* "即期结售汇交割" */, "ficmp0040", IMsgConstant.CURRENCY_EXCHANGE, IMsgConstant.CURRENCY_EXCHANGE_DELIVERY);
            if (result.getInteger("code") == 1) {
                CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
                String serviceStatus = responseHead.getString("service_status");
                String respDescStr = org.apache.commons.lang3.StringUtils.truncate(responseHead.get("service_resp_desc").toString(), 1000);
                currencyExchange.setEnterpriseBackMessg(respDescStr);
                if (("00").equals(serviceStatus)) {
                    responseBody = result.getJSONObject("data").getJSONObject("response_body");
                    String deliveryStatus = (String) responseBody.get("delivery_status");
                    // 01:录入完成(已删除),02:待交割(需手工交割时返回此状态),03:已交割,04:逾期,05：处理中,06:失败
                    switch (deliveryStatus) {
                        case "02":
                            currencyExchange.setEntityStatus(EntityStatus.Update);
                            currencyExchange.setSettlestatus(DeliveryStatus.waitDelivery);
                            MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, currencyExchange);
                            break;
                        case "03":
                            // 已交割，需生成凭证，更新日记账
                            BigDecimal exchangeRate = new BigDecimal(responseBody.getString("cust_rate"));
                            currencyExchange.setSettlestatus(DeliveryStatus.alreadyDelivery);
                            currencyExchange.setSellamount(new BigDecimal(responseBody.getString("sell_amt")));
                            currencyExchange.setPurchaseamount(new BigDecimal(responseBody.getString("buy_amt")));
                            currencyExchange.setExchangerate(exchangeRate);
                            // 取查回已交割状态时的银企返回交割日期
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                            Date delivery_date = responseBody.getString("delivery_date") != null ?
                                    sdf.parse(responseBody.getString("delivery_date")) : new Date();
                            currencyExchange.setSettledate(delivery_date);
                            alreadyDeliveryUpdate(currencyExchange, exchangeRate);
                            break;
                        case "04":
                            currencyExchange.setSettlestatus(DeliveryStatus.beOverdueDelivery);
                            currencyExchange.setEntityStatus(EntityStatus.Update);
                            CurrencyExchange oldBill = findById(CurrencyExchange.ENTITY_NAME, currencyExchange.getId());
                            boolean releaseBudget = cmpBudgetCurrencyExchangeManagerService.releaseBudget(oldBill);
                            if (releaseBudget) {
                                currencyExchange.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                            }
                            MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, currencyExchange);
                            break;
                        case "05":
                            currencyExchange.setSettlestatus(DeliveryStatus.doingDelivery);
                            currencyExchange.setEntityStatus(EntityStatus.Update);
                            MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, currencyExchange);
                            break;
                        case "06":
                            currencyExchange.setSettlestatus(DeliveryStatus.failDelivery);
                            currencyExchange.setEntityStatus(EntityStatus.Update);
                            MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, currencyExchange);
                            break;
                        default:
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102522"), responseBody.get("bank_resp_desc").toString());
                    }
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102523"), responseHead.get("service_resp_desc").toString());
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102524"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806FB", "结售汇交易结果查询接口：SSFE3001 调用异常") /* "结售汇交易结果查询接口：SSFE3001 调用异常" */);
            }
            return result;
        } finally {
            JedisLockUtils.unlockBillWithOutTrace(ymsLock);
        }

    }

    @Override
    public CtmJSONObject currencyExchangeResultQuery(CtmJSONObject param) throws Exception {
        Long currencyExchangeId = Long.valueOf(param.get("id").toString());
        CurrencyExchange currencyExchange = MetaDaoHelper.findById(CurrencyExchange.ENTITY_NAME, currencyExchangeId);
        if (DeliveryType.DirectDelivery.getValue() != currencyExchange.getDeliveryType()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102525"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418071B", "当前单据为非直联，状态同步失败") /* "当前单据为非直联，状态同步失败" */);
        }
        if (DeliveryStatus.doingDelivery != currencyExchange.getSettlestatus() && DeliveryStatus.waitDelivery != currencyExchange.getSettlestatus()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102526"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418071F", "当前单据状态为") /* "当前单据状态为" */ + currencyExchange.getSettlestatus().getName() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418071E", "，状态同步失败") /* "，状态同步失败" */);
        }
        param.put("tran_seq_no", ymsOidGenerator.nextId());
        // 交割状态查询，没有输入UKey密码，通过银企联账户查询客户号
        if (StringUtils.isEmpty(param.getString("customNo"))) {
            String customNo = bankAccountSettingService.getCustomNoByBankAccountId(currencyExchange.getPurchasebankaccount());
            param.put("customNo", customNo);
        }
        String customNo = bankAccountSettingService.getCustomNoAndCheckByBankAccountId(currencyExchange.getPurchasebankaccount(), param.get("customNo"));
        param.put("customNo", customNo);
        CtmJSONObject placeOrderMsg = BankEnterpriseAssociation.buildReqDataSSFE3001(param, currencyExchange);
        String placeOrderString = CtmJSONObject.toJSONString(placeOrderMsg);
        String signMsg = bankConnectionAdapterContext.chanPaySignMessage(placeOrderString);
        List<BasicNameValuePair> requestData = new ArrayList<>();
        requestData.add(new BasicNameValuePair("reqData", placeOrderString));
        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
        CtmJSONObject result = new CtmJSONObject();
        CtmJSONObject responseBody = new CtmJSONObject();
//        if (HttpsUtils.isWhiteUrl(bankConnectionAdapterContext.getChanPayUri())) {
        result = HttpsUtils.doHttpsPostNew(CURRENCY_EXCHANGE_RESULT_QUERY, requestData, bankConnectionAdapterContext.getChanPayUri());
        //添加业务日志
        CtmJSONObject logJsonObject = new CtmJSONObject();
        logJsonObject.put("requestData", requestData);
        logJsonObject.put("responseData", result);
        ctmcmpBusinessLogService.saveBusinessLog(logJsonObject, currencyExchange.getCode() + "CURRENCY_EXCHANGE_RESULT_QUERY", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00110", "结售汇交易结果查询") /* "结售汇交易结果查询" */, "ficmp0040", IMsgConstant.CURRENCY_EXCHANGE, IMsgConstant.CURRENCY_EXCHANGE_RESULT_QUERY);
//        }
        if (result.getInteger("code") == 1) {
            CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
            String serviceStatus = responseHead.getString("service_status");
            if (("00").equals(serviceStatus)) {
                responseBody = result.getJSONObject("data").getJSONObject("response_body");
                String deliveryStatus = (String) responseBody.get("delivery_status");
                String respDescStr = org.apache.commons.lang3.StringUtils.truncate(responseHead.get("service_resp_desc").toString(), 1000);
                currencyExchange.setEnterpriseBackMessg(respDescStr);
                // 01:录入完成(已删除),02:待交割(需手工交割时返回此状态),03:已交割,04:逾期,05：处理中,06:失败
                switch (deliveryStatus) {
                    case "02":
                        currencyExchange.setEntityStatus(EntityStatus.Update);
                        currencyExchange.setSettlestatus(DeliveryStatus.waitDelivery);
                        MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, currencyExchange);
                        break;
                    case "03":
                        // 已交割，需生成凭证，更新日记账
                        BigDecimal exchangeRate = new BigDecimal(responseBody.getString("cust_rate"));
                        currencyExchange.setSettlestatus(DeliveryStatus.alreadyDelivery);
                        currencyExchange.setSellamount(new BigDecimal(responseBody.getString("sell_amt")));
                        currencyExchange.setPurchaseamount(new BigDecimal(responseBody.getString("buy_amt")));
                        currencyExchange.setExchangerate(exchangeRate);
                        // 取查回已交割状态时的银企返回交割日期
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                        Date delivery_date = responseBody.getString("delivery_date") != null ?
                                sdf.parse(responseBody.getString("delivery_date")) : new Date();
                        currencyExchange.setDelayedDate(delivery_date);
                        currencyExchange.setSettledate(delivery_date);
                        alreadyDeliveryUpdate(currencyExchange, exchangeRate);
                        if (currencyExchange.getBilltype() != null && currencyExchange.getBilltype() == EventType.CurrencyExchangeApply.getValue()) {
                            // 外币兑换申请，事项类型的单据，需更新外币申请单据状态
                            currencyApplyService.updateDeliveryStatus(currencyExchange.getCurrencyapplyid(), DeliveryStatus.alreadyDelivery.getValue(), new Date());
                        }
                        break;
                    case "04":
                        currencyExchange.setSettlestatus(DeliveryStatus.beOverdueDelivery);
                        currencyExchange.setEntityStatus(EntityStatus.Update);
                        CurrencyExchange oldBill = findById(CurrencyExchange.ENTITY_NAME, currencyExchange.getId());
                        boolean releaseBudget = cmpBudgetCurrencyExchangeManagerService.releaseBudget(oldBill);
                        if (releaseBudget) {
                            currencyExchange.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                        }
                        MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, currencyExchange);
                        if (currencyExchange.getBilltype() != null && currencyExchange.getBilltype() == EventType.CurrencyExchangeApply.getValue()) {
                            // 外币兑换申请，事项类型的单据，需更新外币申请单据状态
                            currencyApplyService.updateDeliveryStatus(currencyExchange.getCurrencyapplyid(), DeliveryStatus.beOverdueDelivery.getValue(), new Date());
                        }
                        break;
                    case "05":
                        currencyExchange.setSettlestatus(DeliveryStatus.doingDelivery);
                        currencyExchange.setEntityStatus(EntityStatus.Update);
                        MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, currencyExchange);
                        break;
                    case "06":
                        currencyExchange.setSettlestatus(DeliveryStatus.failDelivery);
                        currencyExchange.setEntityStatus(EntityStatus.Update);
                        MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, currencyExchange);
                        if (currencyExchange.getBilltype() != null && currencyExchange.getBilltype() == EventType.CurrencyExchangeApply.getValue()) {
                            // 外币兑换申请，事项类型的单据，需更新外币申请单据状态
                            currencyApplyService.updateDeliveryStatus(currencyExchange.getCurrencyapplyid(), DeliveryStatus.failDelivery.getValue(), new Date());
                        }
                        break;
                    default:
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102524"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806FB", "结售汇交易结果查询接口：SSFE3001 调用异常") /* "结售汇交易结果查询接口：SSFE3001 调用异常" */);
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102527"), responseHead.get("service_resp_desc").toString());
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102524"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806FB", "结售汇交易结果查询接口：SSFE3001 调用异常") /* "结售汇交易结果查询接口：SSFE3001 调用异常" */);
        }
        return responseBody;
    }

    /**
     * 银企联返回已交割状态的外币兑换单，更新业务单据状态，更新日记账，生成凭证
     *
     * @param currencyExchange
     * @param exchangeRate
     * @throws Exception
     */
    private void alreadyDeliveryUpdate(CurrencyExchange currencyExchange, BigDecimal exchangeRate) throws Exception {
        // 设置汇率类型为：用户自定义汇率
        ExchangeRateTypeVO exchangeRateTypeVO = baseRefRpcService.queryExchangeRateRateTypeByCode(CurrencyRateTypeCode.CustomCode.getValue());
        currencyExchange.setExchangeRateType(exchangeRateTypeVO.getId());
        /*if (currencyExchange.getExchangetype() == ExchangeType.Sell) {
            // 卖出外汇
            currencyExchange.setPurchaserate(BigDecimal.ONE);
            currencyExchange.setSellrate(exchangeRate);
        } else if (currencyExchange.getExchangetype() == ExchangeType.Buy) {
            // 买入外汇
            currencyExchange.setPurchaserate(exchangeRate);
            currencyExchange.setSellrate(BigDecimal.ONE);
        }*/
        BigDecimal purchaseLocalAmount = currencyExchange.getPurchaseamount().multiply(currencyExchange.getPurchaserate());
        currencyExchange.setPurchaselocalamount(purchaseLocalAmount.setScale(8,BigDecimal.ROUND_HALF_UP));
        BigDecimal sellLocalAmount = currencyExchange.getSellamount().multiply(currencyExchange.getSellrate());
        currencyExchange.setSellloaclamount(sellLocalAmount.setScale(8,BigDecimal.ROUND_HALF_UP));
        // 如果是外币兑换类型，更新：汇兑损益
        //CZFW-499044【日常】货币兑换api更新数据时，更改买入金额和卖出金额后，没有更新汇兑损益值
//        if (currencyExchange.getExchangetype() == ExchangeType.Exchange) {
            currencyExchange.setExchangeloss(currencyExchange.getSellloaclamount().subtract(currencyExchange.getPurchaselocalamount()));
//        }
        currencyExchange.set("_entityName", CurrencyExchange.ENTITY_NAME);
        currencyExchange.setEntityStatus(EntityStatus.Update);
        // currencyExchange对象最好重新生成
        MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, currencyExchange);
        CurrencyExchange ce = MetaDaoHelper.findById(CurrencyExchange.ENTITY_NAME, currencyExchange.getId());
        boolean implement = cmpBudgetCurrencyExchangeManagerService.implement(ce);
        if (implement) {
            cmpBudgetCurrencyExchangeManagerService.updateOccupyBudget(ce, OccupyBudget.ActualSuccess.getValue());
        }
        cmpVoucherService.generateVoucherWithResult(currencyExchange);
        journalService.updateJournalForExchangeCurrency(currencyExchange);
    }

    @Override
    public CtmJSONObject currencyExchangeRateQuery(CtmJSONObject param) throws Exception {
        //定义 是否询价成功标识，1表示成功，0表示失败
        int RateQueryFlag;
        //生成业务批次号:需要返回给前端，一个单据只有一个业务批次号
        String batch_no = ymsOidGenerator.nextStrId();
        //交易流水号
        String tran_seq_no = ymsOidGenerator.nextStrId();
        //请求流水号
        String requestseqno = ymsOidGenerator.nextStrId();
        param.put("batch_no", batch_no);
        param.put("tran_seq_no", tran_seq_no);
        param.put("requestseqno", requestseqno);

        CtmJSONObject data = param.getJSONObject("data");
        //卖出账号
        String sellbankaccountId = data.getString("sellbankaccount");
        Map<String, Object> sellbankaccountIdMap = QueryBaseDocUtils.queryEnterpriseBankAccountById(sellbankaccountId);
        String sell_acct_no = String.valueOf(sellbankaccountIdMap.get("account"));
        param.put("sell_acct_no", sell_acct_no);
        //买入账号
        String purchasebankaccountId = data.getString("purchasebankaccount");
        Map<String, Object> purchasebankaccountIdMap = QueryBaseDocUtils.queryEnterpriseBankAccountById(purchasebankaccountId);
        String buy_acct_no = String.valueOf(purchasebankaccountIdMap.get("account"));
        param.put("buy_acct_no", buy_acct_no);
        //卖出币种
        CurrencyTenantDTO sellcurrencyTenantDTO = baseRefRpcService.queryCurrencyById(data.getString("sellCurrency"));
        param.put("sell_curr", sellcurrencyTenantDTO.getCode());
        //买入币种
        CurrencyTenantDTO purchasecurrencyTenantDTO = baseRefRpcService.queryCurrencyById(data.getString("purchaseCurrency"));
        param.put("buy_curr", purchasecurrencyTenantDTO.getCode());
        //保证金账号
        if (!StringUtils.isEmpty(data.getString("depositAccountNo"))) {
            String depositAccountNoId = data.getString("depositAccountNo");
            Map<String, Object> depositAccountNoIdMap = QueryBaseDocUtils.queryEnterpriseBankAccountById(depositAccountNoId);
            String deposit_acct_no = String.valueOf(depositAccountNoIdMap.get("account"));
            param.put("deposit_acct_no", deposit_acct_no);
        }
        String customNo = bankAccountSettingService.getCustomNoAndCheckByBankAccountId(purchasebankaccountId, param.get("customNo"));
        param.put("customNo", customNo);
        CtmJSONObject placeOrderMsg = BankEnterpriseAssociation.buildReqDataSSFE1001(param);
        String placeOrderString = CtmJSONObject.toJSONString(placeOrderMsg);
        String signMsg = bankConnectionAdapterContext.chanPaySignMessage(placeOrderString);
        List<BasicNameValuePair> requestData = new ArrayList<>();
        requestData.add(new BasicNameValuePair("reqData", placeOrderString));
        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
        //响应报文 用result2接收返回给前端
        CtmJSONObject result2 = new CtmJSONObject();
//        if (HttpsUtils.isWhiteUrl(bankConnectionAdapterContext.getChanPayUri())) {
        CtmJSONObject result = HttpsUtils.doHttpsPostNew(CURRENCY_EXCHANGE_RATE_QUERY, requestData, bankConnectionAdapterContext.getChanPayUri());
        //添加业务日志
        CtmJSONObject logJsonObject = new CtmJSONObject();
        logJsonObject.put("requestData", requestData);
        logJsonObject.put("responseData", result);
        //业务日志code 记录单据code
        String code = data.getString("code");
        ctmcmpBusinessLogService.saveBusinessLog(logJsonObject, code, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00111", "外币兑换询价") /* "外币兑换询价" */, "ficmp0040", IMsgConstant.CURRENCY_EXCHANGE, IMsgConstant.CURRENCY_EXCHANGE_RATE_QUERY);
        String service_status = result.getJSONObject("data").getJSONObject("response_head").getString("service_status");
        if ("00".equals(service_status)) { //00 表示调用成功
            CtmJSONObject response_body = new CtmJSONObject();
            response_body = result.getJSONObject("data").getJSONObject("response_body");
            //银行响应码
            result2.put("bank_resp_code", response_body.getString("bank_resp_code"));
            //银行响应信息
            result2.put("bank_resp_desc", response_body.getString("bank_resp_desc"));
            //银行营业日期
            result2.put("bank_busi_date", response_body.getString("bank_busi_date"));
            //询价编号
            result2.put("inquiry_id", response_body.getString("inquiry_id"));
            //卖出金额
            result2.put("sellamount", response_body.getString("sell_amt"));
            //买入金额
            result2.put("purchaseamount", response_body.getString("buy_amt"));
            //汇率
            result2.put("inquiryExchangerate", response_body.getString("rate"));
            //牌价
            result2.put("lst_prc", response_body.getString("lst_prc"));
            //客户价
            result2.put("cst_prc", response_body.getString("cst_prc"));
            //业务批次号
            result2.put("batch_no", batch_no);
            RateQueryFlag = 1;
            result2.put("msg", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041806FF", "询价成功!") /* "询价成功!" */);
            result2.put("RateQueryFlag", RateQueryFlag);
        } else {
            String msg = result.getJSONObject("data").getJSONObject("response_head").getString("service_resp_desc");
            RateQueryFlag = 0;
            result2.put("msg", msg);
            result2.put("RateQueryFlag", RateQueryFlag);
        }

//        }
        return result2;
    }


    @Override
    public CtmJSONObject financeCompanyRateQuery(CtmJSONObject params) throws Exception {
        FinanceCompanyRateQueryRequestVO vo = new FinanceCompanyRateQueryRequestVO();
        //交易流水号
        vo.setTran_seq_no(ymsOidGenerator.nextStrId());
        //请求流水号
        vo.setTran_seq_no(ymsOidGenerator.nextStrId());
//        CtmJSONObject data = params.getJSONObject("data");
        String flag = params.getString("flag");
        //期望交割日期
        Date expectedDeliveryDate = params.getDate("expectedDeliveryDate");
        //询汇时去掉期望交割日期
//        if(expectedDeliveryDate != null){
//            vo.setDesired_date(DateUtils.dateFormat(expectedDeliveryDate,"yyyyMMdd"));
//        }else {
//            vo.setDesired_date(DateUtils.dateFormat(new Date(),"yyyyMMdd"));
//        }
        String bankAccountId;
        if ("0".equals(flag)) {
            //买入账号
            bankAccountId = params.getString("purchasebankaccount");
            EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(bankAccountId);
            if (enterpriseBankAcctVO != null) {
                String bankTypeId = enterpriseBankAcctVO.getBank();
                BankVO bankVO = enterpriseBankQueryService.querybankTypeNameById(bankTypeId);
                if (Objects.isNull(bankVO) || !FINANCE_COMPANY_BANK_TYPE_CODE.equals(bankVO.getCode())) {
                    return null;
                }
            }
            vo.setAcct_no(enterpriseBankAcctVO.getAccount());
            vo.setAcct_name(enterpriseBankAcctVO.getAcctName());
            vo.setDealt_side("BUY");
        } else {
            //卖出账号
            bankAccountId = params.getString("sellbankaccount");
            EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(bankAccountId);
            if (enterpriseBankAcctVO != null) {
                String bankTypeId = enterpriseBankAcctVO.getBank();
                BankVO bankVO = enterpriseBankQueryService.querybankTypeNameById(bankTypeId);
                if (Objects.isNull(bankVO) || !FINANCE_COMPANY_BANK_TYPE_CODE.equals(bankVO.getCode())) {
                    return null;
                }
            }
            vo.setAcct_no(enterpriseBankAcctVO.getAccount());
            vo.setAcct_name(enterpriseBankAcctVO.getAcctName());
            vo.setDealt_side("SELL");
        }
        if (params.getString("purchaseCurrency") == null || params.getString("sellCurrency") == null) {
            // 买入币种或卖出币种为空，就不必查询汇率了
            return new CtmJSONObject();
        }
        CurrencyTenantDTO purchaseCurrency = currencyQueryService.findById(params.getString("purchaseCurrency"));
        CurrencyTenantDTO sellCurrency = currencyQueryService.findById(params.getString("sellCurrency"));
        //202403 司库调用【汇率查询SSFE3012】接口时：若交易类型=买入外汇，接口“原币种”=司库.买入币种，接口“兑换币种”=司库.卖出币种；
        // 若交易类型=卖出外汇，接口“原币种”=司库.卖出币种，接口“兑换币种”=司库.买入币种
        if ("0".equals(flag)) {
            //原币种
            vo.setSell_curr(purchaseCurrency.getCode());
            //兑换币种
            vo.setBuy_curr(sellCurrency.getCode());
        } else {
            //原币种
            vo.setSell_curr(sellCurrency.getCode());
            //兑换币种
            vo.setBuy_curr(purchaseCurrency.getCode());
        }
        String customNo = bankAccountSettingService.getCustomNoByBankAccountId(bankAccountId);
        vo.setCustomNo(customNo);
        CtmJSONObject placeOrderMsg = BankEnterpriseAssociation.buildReqDataSSFE3012(vo);
        String placeOrderString = CtmJSONObject.toJSONString(placeOrderMsg);
        String signMsg = bankConnectionAdapterContext.chanPaySignMessage(placeOrderString);
        List<BasicNameValuePair> requestData = new ArrayList<>();
        requestData.add(new BasicNameValuePair("reqData", placeOrderString));
        requestData.add(new BasicNameValuePair("reqSignData", signMsg));
        //响应报文 用result2接收返回给前端
        CtmJSONObject result2 = new CtmJSONObject();
//        if (HttpsUtils.isWhiteUrl(bankConnectionAdapterContext.getChanPayUri())) {
        CtmJSONObject result = HttpsUtils.doHttpsPostNew(FINANCE_COMPANY_RATE_QUERY, requestData, bankConnectionAdapterContext.getChanPayUri());
        //添加业务日志
        CtmJSONObject logJsonObject = new CtmJSONObject();
        logJsonObject.put("requestData", requestData);
        logJsonObject.put("responseData", result);
        //业务日志code 记录单据code
        ctmcmpBusinessLogService.saveBusinessLog(logJsonObject, null, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005BE", "财务公司汇率查询") /* "财务公司汇率查询" */, "ficmp0040", IMsgConstant.CURRENCY_EXCHANGE, IMsgConstant.CURRENCY_EXCHANGE_RATE_QUERY);
        String service_status = result.getJSONObject("data").getJSONObject("response_head").getString("service_status");
        if (result.getInteger("code") == 1) {
            if ("00".equals(service_status)) { //00 表示调用成功
                CtmJSONObject response_body = new CtmJSONObject();
                response_body = result.getJSONObject("data").getJSONObject("response_body");
                if (response_body == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102528"), result.getJSONObject("data").getJSONObject("response_head").getString("service_resp_desc"));
                }
                // record，传入买入币种和卖出币种的时候，应该只返回一条数据
                ArrayList<Map<String, String>> records = (ArrayList) response_body.get("record");
                //银行响应码
                result2.put("bank_resp_code", response_body.getString("bank_resp_code"));
                //银行响应信息
                result2.put("bank_resp_desc", response_body.getString("bank_resp_desc"));
                if (records == null || records.size() == 0) {
                    // 未返回有效数据
                    result2.put("msg", "fail");
                } else if (records.size() == 1) {
                    // 202404 成交汇率的取值：若交易类型=买入外汇，取银企联返回的“卖出价”；若交易类型=卖出外汇，取银企联返回的“买入价”；
                    if ("0".equals(flag)) { //买入外汇
                        result2.put("exchangerate", records.get(0).get("selling_rate"));
                    } else { //卖出外汇
                        result2.put("exchangerate", records.get(0).get("buying_rate"));
                    }
                    result2.put("msg", "success");
                } else {
                    // 招商银行等即使上传了币种也返回汇率列表，必须自行筛选过滤
                    for (Map<String, String> record : records) {
                        if (purchaseCurrency.getCode().equals(record.get("buy_curr")) && sellCurrency.getCode().equals(record.get("sell_curr"))) {
                            result2.put("exchangerate", record.get("selling_rate"));
                            result2.put("msg", "success");
                        }
                    }
                }
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102529"), result.getJSONObject("data").getJSONObject("response_head").getString("service_resp_desc"));
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102530"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1A7299180588000E", "实时汇率详情查询接口：SSFE3012 调用异常") /* "实时汇率详情查询接口：SSFE3012 调用异常" */);
        }
//        }
        return result2;
    }

    @Override
    public void insertCurrencyApply(CurrencyApply currencyApply) throws Exception {
        CurrencyExchange currencyExchange = new CurrencyExchange();
        //去掉特征传递
        currencyApply.setCharacterDef(null);
        BeanUtils.copyProperties(currencyApply, currencyExchange);
        currencyExchange.setId(ymsOidGenerator.nextId());
        // 外币兑换申请id
        currencyExchange.setCurrencyapplyid(currencyApply.getId());
        currencyExchange.setCurrencyapplynumber(currencyApply.getCode());
        // 交易类型转换（***生单只能生成基础交易类型，不能生成自定义交易类型***）
        BdTransType bdTransType;
        if (ExchangeType.Buy.getValue() == currencyApply.getExchangetype()) {
            bdTransType = transTypeQueryService.queryTransTypes(CurrencyExchangeTransCode.BUY);
            currencyExchange.setFlag(Bsflag.Buy);
        } else if (ExchangeType.Sell.getValue() == currencyApply.getExchangetype()) {
            bdTransType = transTypeQueryService.queryTransTypes(CurrencyExchangeTransCode.SELL);
            currencyExchange.setFlag(Bsflag.Sell);
        } else if (ExchangeType.Exchange.getValue() == currencyApply.getExchangetype()) {
            bdTransType = transTypeQueryService.queryTransTypes(CurrencyExchangeTransCode.EXCHANGE);
            currencyExchange.setFlag(Bsflag.Exchange);
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102531"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_192DFBE20488000C", "兑换类型错误，生成外币兑换单失败！") /* "兑换类型错误，生成外币兑换单失败！" */);
        }
        currencyExchange.setTradetype(bdTransType.getId());
        // 汇兑损益计算
        if (currencyApply.getSellloaclamount() != null && currencyApply.getPurchaselocalamount() != null) {
            currencyExchange.setExchangeloss(currencyApply.getSellloaclamount().subtract(currencyApply.getPurchaselocalamount()));
        }
        // 初始化单据信息默认值
        currencyExchange.setBilltype(EventType.CurrencyExchangeApply.getValue());
        currencyExchange.setBillstatus(BillStatus.Created);
        currencyExchange.setAuditstatus(AuditStatus.Incomplete);
        currencyExchange.setSettlestatus(DeliveryStatus.find(currencyApply.getDeliverystatus()));
        currencyExchange.setVoucherstatus(VoucherStatus.Empty);
        currencyExchange.setCreator(currencyApply.getAuditor());
        currencyExchange.setCreatorId(currencyApply.getAuditorId());
        currencyExchange.setCreateDate(new Date());
        currencyExchange.setCreateTime(new Date());
        currencyExchange.setVerifystate(VerifyState.INIT_NEW_OPEN.getValue());
        currencyExchange.setSrcitem(EventSource.Cmpchase);
        currencyExchange.setStatus(Status.newopen);
        currencyExchange.setAuditstatus(AuditStatus.Incomplete);
        currencyExchange.setAuditor(null);
        currencyExchange.setAuditorId(null);
        currencyExchange.setAuditDate(null);
        currencyExchange.setAuditTime(null);
        //货币兑换申请单手续费相关字段传递给货币兑换单--start
        currencyExchange.setCommissionamount(currencyApply.getCommissionamount());
        currencyExchange.setCommissionbankaccount(currencyApply.getCommissionbankaccount());
        currencyExchange.setCommissioncashaccount(currencyApply.getCommissioncashaccount());
        currencyExchange.setCommissionCurrency(currencyApply.getCommissionCurrency());
        currencyExchange.setCommissionlocalamount(currencyApply.getCommissionlocalamount());
        currencyExchange.setCommissionrate(currencyApply.getCommissionrate());
        //货币兑换申请单手续费相关字段传递给货币兑换单--end
        ProcessDefinitionQueryParam param = new ProcessDefinitionQueryParam();
        param.setCategory("");
        param.setBillTypeId(ICmpConstant.CM_CMP_CURRENCYEXCHANGE);
        param.setOrgId(currencyExchange.getAccentity());
        RepositoryService repositoryService = processService.bpmRestServices().getRepositoryService();
        Object result = repositoryService.checkProcessDefinition(param);
        if (!((ObjectNode) result).get("hasProcessDefinition").booleanValue()) {
            currencyExchange.setIsWfControlled(false);
        } else {
            currencyExchange.setIsWfControlled(true);
        }
        currencyExchange.setEntityStatus(EntityStatus.Insert);
        String code = getBillCode(currencyExchange);
        currencyExchange.setCode(code);
        CmpMetaDaoHelper.insert(CurrencyExchange.ENTITY_NAME, currencyExchange);

        addJounal(currencyExchange);

    }

    /**
     * 获取货币兑换单据编码
     * @param currencyExchange
     * @return
     */
    private String getBillCode(CurrencyExchange currencyExchange){
        IBillCodeComponentService billCodeComponentService = AppContext.getBean(IBillCodeComponentService.class);
        String ytenantid = AppContext.getBean(IBillCodeSupport.class).getYtenantId();
        BillCodeComponentParam billCodeComponentParam = new BillCodeComponentParam(
                CmpBillCodeMappingConfUtils.getBillCode(IBillNumConstant.CURRENCYEXCHANGE),
                IBillNumConstant.CURRENCYEXCHANGE,ytenantid,
                null,null,new BillCodeObj[]{new BillCodeObj(currencyExchange)});
        String[] codelist = billCodeComponentService.getBatchBillCodes(billCodeComponentParam);
        if(codelist!=null && codelist.length>0){
            return codelist[0];
        }else {
            return null;
        }
    }

    private void addJounal(CurrencyExchange currencyExchange) throws Exception {
        //转换过程
        Journal journal = createJounal(currencyExchange, IBillNumConstant.CURRENCYEXCHANGE, 0);
        Journal journal2 = createJounal(currencyExchange, IBillNumConstant.CURRENCYEXCHANGE, 1);
        Journal journal3 = null;
        if (null != currencyExchange.get("commissionamount") && (null != currencyExchange.get("commissionbankaccount") || null != currencyExchange.get("commissioncashaccount"))) {
            journal3 = createJounal(currencyExchange, IBillNumConstant.CURRENCYEXCHANGE, 2);
        }

        if (!((StringUtils.isEmpty(journal.getBankaccount()) || StringUtils.isEmpty(journal2.getBankaccount()))
                && (StringUtils.isEmpty(journal.getCashaccount()) || StringUtils.isEmpty(journal2.getCashaccount())))) {

            //回退逻辑
            CmpWriteBankaccUtils.delAccountBook(currencyExchange.getId().toString());
            //生成日记账
            cmpWriteBankaccUtils.addAccountBook(journal);
            cmpWriteBankaccUtils.addAccountBook(journal2);
            cmpWriteBankaccUtils.addAccountBook(journal3);
        }
    }

    @Override
    public Boolean deleteCurrencyApply(Long currencyApplyId) throws Exception {
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("currencyapplyid").eq(currencyApplyId));
        QuerySchema schema = QuerySchema.create().addSelect("id, verifystate, paybankbill, collectbankbill, paybillclaim, collectbillclaim");
        schema.addCondition(group);
        List<Map<String, Object>> currencyExchangeList = MetaDaoHelper.query(CurrencyExchange.ENTITY_NAME, schema);
        if (currencyExchangeList != null && currencyExchangeList.size() > 0) {
            // 一个外币申请单id只对应一条外币兑换数据
            Map currencyExchange = currencyExchangeList.get(0);
            Long id = (Long) currencyExchange.get("id");
            // 审批流状态
            Short verifyState = Short.parseShort(currencyExchange.get("verifystate").toString());
            if (verifyState == VerifyState.INIT_NEW_OPEN.getValue() || verifyState == VerifyState.REJECTED_TO_MAKEBILL.getValue() || verifyState == VerifyState.TERMINATED.getValue()) {
                MetaDaoHelper.deleteByObjectId(CurrencyExchange.ENTITY_NAME, id);
                //删除日记账
                CmpWriteBankaccUtils.delAccountBook(id.toString());
                delRelation(currencyExchange);
                return true;
            } else {
                throw new CtmException(MessageFormat.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800B2", "单据状态为{0}，不能删除") /* "单据状态为{0}，不能删除" */, VerifyState.find(verifyState).getName()));
            }
        }
        return false;
    }

    /**
     * 判断删除货币兑换时，若有关联数据，需要释放关联信息
     *
     * @param currencyExchange
     * @throws Exception
     */
    private void delRelation(Map<String, Object> currencyExchange) throws Exception {
        String billId = currencyExchange.get("id").toString();
        List<CtmJSONObject> listreq = new ArrayList<>();
        //买入关联银行对账单，封装关联删除数据
        if (currencyExchange.get("paybankbill") != null) {
            CtmJSONObject jsonReq = new CtmJSONObject();
            jsonReq.put("busid", currencyExchange.get("paybankbill"));
            if (currencyExchange.get("collectbankbill") == null) {
                //俩都是流水的时候传一个就行了
                jsonReq.put("stwbbusid", billId);
            }
            listreq.add(jsonReq);
        }
        //卖出关联银行对账单，封装关联删除数据
        if (currencyExchange.get("collectbankbill") != null) {
            CtmJSONObject jsonReq = new CtmJSONObject();
            jsonReq.put("busid", currencyExchange.get("collectbankbill"));
            jsonReq.put("stwbbusid", billId);
            listreq.add(jsonReq);
        }
        //买入关联认领单，封装关联删除数据
        if (currencyExchange.get("paybillclaim") != null) {
            CtmJSONObject jsonReq = new CtmJSONObject();
            jsonReq.put("claimid", currencyExchange.get("paybillclaim"));
            if (currencyExchange.get("collectbillclaim")  != null){
                //俩都是认领的时候传一个就行了
                jsonReq.put("stwbbusid", billId);
            }
            listreq.add(jsonReq);
        }
        //卖出关联认领单，封装关联删除数据
        if (currencyExchange.get("collectbillclaim") != null) {
            CtmJSONObject jsonReq = new CtmJSONObject();
            jsonReq.put("claimid", currencyExchange.get("collectbillclaim"));
            jsonReq.put("stwbbusid", billId);
            listreq.add(jsonReq);
        }
        if (listreq.size() > 0) {
            for (CtmJSONObject jsonReq : listreq) {
                reWriteBusCorrDataService.resDelData(jsonReq);
            }
        }
    }

    @Override
    public CtmJSONObject checkAddTransType(CtmJSONObject params) throws Exception {
        String serviceCode = params.getString("serviceCode");
        String addType = params.getString("addType");
        String tradeType = serviceCode.split("_")[0];
        BdTransType bdTransType = transTypeQueryService.findById(tradeType);
        CtmJSONObject jsonObject = CtmJSONObject.parseObject(bdTransType.getExtendAttrsJson());
        boolean addTransTypeFlag = false;
        if ("buyin".equals(jsonObject.get("transferType_wbdh"))) {
            // 买入外汇
            if (!"0".equals(addType)) {
                addTransTypeFlag = true;
            }
        } else if ("sellout".equals(jsonObject.get("transferType_wbdh"))) {
            // 卖出外汇
            if (!"1".equals(addType)) {
                addTransTypeFlag = true;
            }
        } else if ("bs".equals(jsonObject.get("transferType_wbdh"))) {
            // 货币兑换
            if (!"2".equals(addType)) {
                addTransTypeFlag = true;
            }
        }
        params.put("addTransTypeFlag", addTransTypeFlag);
        return params;
    }

    /**
     * 交割文件上传银企联
     *
     * @param param            页面信息
     * @param currencyExchange 上传的货币兑换单信息
     * @return
     */
    private CtmJSONArray uploadDeliveryFile(CtmJSONObject param, CurrencyExchange currencyExchange) throws Exception {
        CtmJSONArray fileList = new CtmJSONArray();
        CooperationFileService cooperationFileService = AppContext.getBean(CooperationFileService.class);
        List<CooperationFileInfo> fileInfos = cooperationFileService.queryBusinessFiles("yonbip-fi-ctmcmp", currencyExchange.getId().toString(), AppContext.getTenantId().toString());
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(fileInfos)) {
            log.error("未查询到附件信息");
            return fileList;
        }

        //调用银企联接口11SC01上传单个附件。需要循环遍历调用
        for (CooperationFileInfo fileInfo : fileInfos) {
            CtmJSONObject fileInfoResult = new CtmJSONObject();
            CtmJSONObject placeOrderMsg = BankEnterpriseAssociation.buildReqData11SC01(param, currencyExchange, fileInfo);
            String placeOrderString = CtmJSONObject.toJSONString(placeOrderMsg);
            String signMsg = bankConnectionAdapterContext.chanPaySignMessage(placeOrderString);
            List<BasicNameValuePair> requestData = new ArrayList<>();
            requestData.add(new BasicNameValuePair("reqData", placeOrderString));
            requestData.add(new BasicNameValuePair("reqSignData", signMsg));
            CtmJSONObject result = HttpsUtils.doHttpsPost(UPLOAD_FILE, requestData, bankConnectionAdapterContext.getChanPayUri());
            CtmJSONObject logJsonObject = new CtmJSONObject();
            logJsonObject.put("requestData", requestData);
            logJsonObject.put("responseData", result);
            ctmcmpBusinessLogService.saveBusinessLog(logJsonObject, currencyExchange.getCode() + "_UPLOADFILE" + fileInfo.getFileId(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005AA", "外币兑换附件上传") /* "外币兑换附件上传" */, "ficmp0040", IMsgConstant.CURRENCY_EXCHANGE, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005AA", "外币兑换附件上传") /* "外币兑换附件上传" */);
            if (result.getInteger("code") == 1) {
                //6.获取返回信息，更新数据
                CtmJSONObject responseHead = result.getJSONObject("data").getJSONObject("response_head");
                CtmJSONObject responseBody = result.getJSONObject("data").getJSONObject("response_body");
                String serviceStatus = responseHead.getString("service_status");
                String serviceRespDesc = responseHead.getString("service_resp_desc");
                //6.1serviceStatus为服务状态，00表示服务成功，其他均为失败
                if (("00").equals(serviceStatus)) {
                    //6.2获取子表返回信息
                    String fileName = responseBody.getString("file_name");
                    fileInfoResult.put("org_file_name", fileName);
                    fileList.add(fileInfoResult);
                } else {
                    log.error("附件上传银企联异常，message:{}", serviceRespDesc);//@notranslate
                }
            }
        }

        return fileList;
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public String budgetCheckNew(CmpBudgetVO cmpBudgetVO) throws Exception {
        if (!cmpBudgetManagerService.isCanStart(IBillNumConstant.CURRENCYEXCHANGE)) {
            CtmJSONObject resultBack = new CtmJSONObject();
            resultBack.put(ICmpConstant.CODE, true);
            return ResultMessage.data(resultBack);
        }
        String billnum = cmpBudgetVO.getBillno();

        String entityname = null;
        BillMapEnum enumByBillNum = BillMapEnum.getEnumByBillNum(billnum);
        if (enumByBillNum != null) {
            entityname = enumByBillNum.getEntityName();
        }
        if (com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(entityname)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100612"), InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000CF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005AC", "请求参数缺失") /* "请求参数缺失" */));
        }

        //TODO 变更单据返回来源单据信息，ids 为便跟单据自己信息
        List<String> ids = cmpBudgetVO.getIds();
        List<BizObject> bizObjects = new ArrayList<>();
        if (ValueUtils.isNotEmptyObj(ids)) {
            bizObjects = queryBizObjsWarpParentInfo(ids);
        } else if (ValueUtils.isNotEmptyObj(cmpBudgetVO.getBizObj())) {
            BizObject bizObject = CtmJSONObject.parseObject(cmpBudgetVO.getBizObj(), BizObject.class);
            bizObjects.add(bizObject);
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100612"), InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000CF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005AC", "请求参数缺失") /* "请求参数缺失" */));
        }
        //变更单据
        String changeBillno = cmpBudgetVO.getChangeBillno();
        if (!com.yonyoucloud.fi.cmp.util.StringUtils.isEmpty(changeBillno)) {
            if (CollectionUtils.isEmpty(bizObjects)) {
                CtmJSONObject resultBack = new CtmJSONObject();
                resultBack.put(ICmpConstant.CODE, true);
                resultBack.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000D0", "变更金额小于原金额,不需要校验!"));
                return ResultMessage.data(resultBack);
            }
            //变更单据获取（融资登记单据类型）
            billnum = changeBillno;
        } else {
            //非变更单据 自己单据
            if (CollectionUtils.isEmpty(bizObjects)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100612"), InternationalUtils.getMessageWithDefault("UID:P_TLM-BE_19AF9FC204D000CF", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005AC", "请求参数缺失") /* "请求参数缺失" */));
            }
        }
        return cmpBudgetCommonManagerService.budgetCheckNew(bizObjects, billnum, BudgetUtils.SUBMIT);
    }

    public List<BizObject> queryBizObjsWarpParentInfo(List<String> ids) throws Exception {
        // 根据id批量查询数据
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
        // 只查询来源为现金的数据 只有这类数据需要升级
        conditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        schema.addCondition(conditionGroup);
        return MetaDaoHelper.queryObject(CurrencyExchange.ENTITY_NAME, schema, null);
    }

    /**
     * 如果是预占就跳过，如果是实占，删除实占，重新预占
     *
     * @param currencyExchange
     * @throws Exception
     */
    private Short budgetAfterUnSettle(CurrencyExchange currencyExchange) throws Exception {
        Short budgeted = currencyExchange.getIsOccupyBudget();
        // 已经释放仍要释放，直接跳过不执行了
        if (budgeted == null || ((budgeted == OccupyBudget.UnOccupy.getValue()))) {
            return null;
        } else if (OccupyBudget.ActualSuccess.getValue() == budgeted) {//是否占预算为实占成功时，删除实占；
            boolean releaseImplement = cmpBudgetCurrencyExchangeManagerService.releaseImplement(currencyExchange);
            if (releaseImplement) {
                //重新预占
                log.error("重新预占.....");
                //且结算状态应置为待结算、并清空结算成功时间
                currencyExchange.setIsOccupyBudget(OccupyBudget.UnOccupy.getValue());
                boolean budget = cmpBudgetCurrencyExchangeManagerService.budget(currencyExchange);
                if (budget) {//可能是没有匹配上规则，也可能是没有配置规则
                    return OccupyBudget.PreSuccess.getValue();
                } else {
                    return OccupyBudget.UnOccupy.getValue();
                }
            } else {
                log.error("释放实占失败,releaseImplement:{}", releaseImplement);
            }
        }
        return null;
    }

    public Journal createJounal(BizObject bizObject, String billno, int data) throws Exception {
//		String serverUrl = AppContext.getEnvConfig("fifrontservername");
        Journal journal = new Journal();
        journal.set(IBussinessConstant.ACCENTITY, bizObject.get(IBussinessConstant.ACCENTITY));
        journal.set("period", bizObject.get("period"));
        journal.set("description", bizObject.get("description"));
        journal.set("srcitem", bizObject.get("srcitem"));
        journal.set("billtype", bizObject.get("billtype"));
        journal.set("tradetype", bizObject.get("tradetype"));
        journal.set("settlemode", bizObject.get("settlemode"));
        journal.setCaobject(bizObject.get("caobject"));
        journal.set("debitoriSum", BigDecimal.ZERO);
        journal.set("debitnatSum", BigDecimal.ZERO);
        journal.set("creditnatSum", BigDecimal.ZERO);
        journal.set("creditoriSum", BigDecimal.ZERO);
        /* 根据币种id查询原币币种信息 */
        List<Map<String, Object>> accEntity = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(bizObject.get(IBussinessConstant.ACCENTITY));
        CurrencyTenantDTO currencyTenantDTO;
        if (accEntity.size() != 0) {
            currencyTenantDTO = baseRefRpcService.queryCurrencyById(accEntity.get(0).get("currency").toString());
        } else {
            // 未获取到会计主体默认币种，不能去单据币种，应直接报错
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101049"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050043", "获取资金组织对应会计主体默认币种失败") /* "获取资金组织对应会计主体默认币种失败" */);
        }
        if (data == 0) {//买入银行账户
            CurrencyTenantDTO currencybuy = baseRefRpcService.queryCurrencyById(bizObject.getString("purchaseCurrency"));
            // 兼容导入覆盖更新
            if (bizObject.get("flag") != null) {
                if (bizObject.get("flag").equals(Bsflag.Sell.getValue())) {//卖出外汇
                    journal.set("debitnatSum", ((BigDecimal) bizObject.get("purchaselocalamount")).setScale(currencyTenantDTO.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("debitoriSum", ((BigDecimal) bizObject.get("purchaseamount")).setScale(currencybuy.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("natbalance", ((BigDecimal) bizObject.get("purchaselocalamount")).setScale(currencyTenantDTO.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("oribalance", ((BigDecimal) bizObject.get("purchaseamount")).setScale(currencybuy.getMoneydigit(), RoundingMode.HALF_UP));
                } else if (bizObject.get("flag").equals(Bsflag.Buy.getValue()) || bizObject.get("flag").equals(Bsflag.Exchange.getValue())) {//买入外汇//货币兑换
                    journal.set("oribalance", ((BigDecimal) bizObject.get("purchaseamount")).setScale(currencybuy.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("natbalance", ((BigDecimal) bizObject.get("purchaselocalamount")).setScale(currencyTenantDTO.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("debitoriSum", ((BigDecimal) bizObject.get("purchaseamount")).setScale(currencybuy.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("debitnatSum", ((BigDecimal) bizObject.get("purchaselocalamount")).setScale(currencyTenantDTO.getMoneydigit(), RoundingMode.HALF_UP));
                }
            }
            if (!StringUtils.isEmpty(bizObject.get("purchasecashaccount"))) {
                /* 根据账户id查询对应现金账号  */
                EnterpriseCashVO enterpriseCashVO = baseRefRpcService.queryEnterpriseCashAcctById(bizObject.getString("purchasecashaccount"));
                journal.setCashaccountno(enterpriseCashVO.getCode());
                journal.set("cashaccount", bizObject.get("purchasecashaccount"));
            }
            if (!StringUtils.isEmpty(bizObject.get("purchasebankaccount"))) {
                journal.setBankaccountno((baseRefRpcService.queryEnterpriseBankAccountById(bizObject.get("purchasebankaccount"))).getAccount());
                journal.set("bankaccount", bizObject.get("purchasebankaccount"));
            }
            journal.set("direction", Direction.Debit.getValue());
            journal.set("currency", bizObject.get("purchaseCurrency"));
            journal.set("exchangerate", bizObject.get("purchaserate"));
        } else if (data == 1) {//卖出银行账户
            CurrencyTenantDTO currencysell = baseRefRpcService.queryCurrencyById(bizObject.getString("sellCurrency"));
            journal.set("debitoriSum", BigDecimal.ZERO);
            journal.set("debitnatSum", BigDecimal.ZERO);
            // 兼容导入覆盖更新
            if (bizObject.get("flag") != null) {
                if (bizObject.get("flag").equals(Bsflag.Sell.getValue()) || bizObject.get("flag").equals(Bsflag.Exchange.getValue())) {//卖出外汇//货币兑换 原币
                    journal.set("oribalance", ((BigDecimal) bizObject.get("sellamount")).setScale(currencysell.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("natbalance", ((BigDecimal) bizObject.get("sellloaclamount")).setScale(currencyTenantDTO.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("creditoriSum", ((BigDecimal) bizObject.get("sellamount")).setScale(currencysell.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("creditnatSum", ((BigDecimal) bizObject.get("sellloaclamount")).setScale(currencyTenantDTO.getMoneydigit(), RoundingMode.HALF_UP));
                } else if (bizObject.get("flag").equals(Bsflag.Buy.getValue())) {//买入外汇
                    journal.set("oribalance", ((BigDecimal) bizObject.get("sellamount")).setScale(currencysell.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("natbalance", ((BigDecimal) bizObject.get("sellloaclamount")).setScale(currencyTenantDTO.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("creditoriSum", ((BigDecimal) bizObject.get("sellamount")).setScale(currencysell.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("creditnatSum", ((BigDecimal) bizObject.get("sellloaclamount")).setScale(currencyTenantDTO.getMoneydigit(), RoundingMode.HALF_UP));
                }
            }
            if (!StringUtils.isEmpty(bizObject.get("sellcashaccount"))) {
                journal.setCashaccountno(baseRefRpcService.queryEnterpriseCashAcctById(bizObject.get("sellcashaccount")).getCode());
                journal.set("cashaccount", bizObject.get("sellcashaccount"));
            }
            if (!StringUtils.isEmpty(bizObject.get("sellbankaccount"))) {
                EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(bizObject.get("sellbankaccount"));
                journal.setBankaccountno(enterpriseBankAcctVO.getAccount());
                journal.set("bankaccount", bizObject.get("sellbankaccount"));
            }
            journal.set("direction", Direction.Credit.getValue());
            journal.set("currency", bizObject.get("sellCurrency"));
            journal.set("exchangerate", bizObject.get("sellrate"));
        } else {//手续费账户
            CurrencyTenantDTO currencycommi;
            if (bizObject.get("commissionCurrency") == null) {
                EnterpriseBankAcctVO commissionbankaccount = baseRefRpcService.queryEnterpriseBankAccountById(bizObject.get("commissionbankaccount"));
                currencycommi = baseRefRpcService.queryCurrencyById(commissionbankaccount.getCurrencyList().get(0).getCurrency());
            } else {
                currencycommi = baseRefRpcService.queryCurrencyById(bizObject.get("commissionCurrency"));
            }
            journal.set("debitoriSum", BigDecimal.ZERO);
            journal.set("debitnatSum", BigDecimal.ZERO);
            if (bizObject.get("flag") != null && bizObject.get("flag").equals(Bsflag.Sell.getValue())) {
                if (!StringUtils.isEmpty(bizObject.get("sellbankaccount")) && bizObject.get("sellbankaccount").equals(bizObject.get("commissionbankaccount"))) {
                    journal.set("oribalance", ((BigDecimal) bizObject.get("commissionamount")).setScale(currencycommi.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("natbalance", ((BigDecimal) bizObject.get("commissionlocalamount")).setScale(currencyTenantDTO.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("creditoriSum", ((BigDecimal) bizObject.get("commissionamount")).setScale(currencycommi.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("creditnatSum", ((BigDecimal) bizObject.get("commissionlocalamount")).setScale(currencyTenantDTO.getMoneydigit(), RoundingMode.HALF_UP));
                } else {
                    journal.set("oribalance", ((BigDecimal) bizObject.get("commissionamount")).setScale(currencycommi.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("natbalance", ((BigDecimal) bizObject.get("commissionlocalamount")).setScale(currencyTenantDTO.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("creditoriSum", ((BigDecimal) bizObject.get("commissionamount")).setScale(currencycommi.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("creditnatSum", ((BigDecimal) bizObject.get("commissionlocalamount")).setScale(currencyTenantDTO.getMoneydigit(), RoundingMode.HALF_UP));
                }
            } else if (bizObject.get("flag") != null && bizObject.get("flag").equals(Bsflag.Buy.getValue())) {
                if (!StringUtils.isEmpty(bizObject.get("sellbankaccount")) && bizObject.get("sellbankaccount").equals(bizObject.get("commissionbankaccount"))) {
                    journal.set("oribalance", ((BigDecimal) bizObject.get("commissionamount")).setScale(currencycommi.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("natbalance", ((BigDecimal) bizObject.get("commissionlocalamount")).setScale(currencyTenantDTO.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("creditoriSum", ((BigDecimal) bizObject.get("commissionamount")).setScale(currencycommi.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("creditnatSum", ((BigDecimal) bizObject.get("commissionlocalamount")).setScale(currencyTenantDTO.getMoneydigit(), RoundingMode.HALF_UP));
                } else {
                    journal.set("oribalance", ((BigDecimal) bizObject.get("commissionamount")).setScale(currencycommi.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("natbalance", ((BigDecimal) bizObject.get("commissionlocalamount")).setScale(currencyTenantDTO.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("creditoriSum", ((BigDecimal) bizObject.get("commissionamount")).setScale(currencycommi.getMoneydigit(), RoundingMode.HALF_UP));
                    journal.set("creditnatSum", ((BigDecimal) bizObject.get("commissionlocalamount")).setScale(currencyTenantDTO.getMoneydigit(), RoundingMode.HALF_UP));
                }
            } else {
                journal.set("oribalance", ((BigDecimal) bizObject.get("commissionamount")).setScale(currencycommi.getMoneydigit(), RoundingMode.HALF_UP));
                journal.set("natbalance", ((BigDecimal) bizObject.get("commissionlocalamount")).setScale(currencyTenantDTO.getMoneydigit(), RoundingMode.HALF_UP));
                journal.set("creditoriSum", ((BigDecimal) bizObject.get("commissionamount")).setScale(currencycommi.getMoneydigit(), RoundingMode.HALF_UP));
                journal.set("creditnatSum", ((BigDecimal) bizObject.get("commissionlocalamount")).setScale(currencyTenantDTO.getMoneydigit(), RoundingMode.HALF_UP));
            }
            if (!StringUtils.isEmpty(bizObject.get("commissioncashaccount"))) {
                journal.setCashaccountno(baseRefRpcService.queryEnterpriseCashAcctById(bizObject.get("commissioncashaccount")).getCode());
                journal.set("cashaccount", bizObject.get("commissioncashaccount"));
            }
            if (!StringUtils.isEmpty(bizObject.get("commissionbankaccount"))) {
                journal.setBankaccountno((baseRefRpcService.queryEnterpriseBankAccountById(bizObject.get("commissionbankaccount"))).getAccount());
                journal.set("bankaccount", bizObject.get("commissionbankaccount"));
            }
            journal.set("direction", Direction.Credit.getValue());
            journal.set("currency", bizObject.get("commissionCurrency"));
            journal.set("exchangerate", bizObject.get("commissionrate"));
        }
        journal.set("dzdate", bizObject.get("dzdate"));
        journal.set("vouchdate", bizObject.get("vouchdate"));
        journal.set("bankbilltype", "");
        journal.set("dept", bizObject.get("dept"));
        journal.set("checkflag", false);
        journal.set("insidecheckflag", false);
        journal.set("auditstatus", AuditStatus.Incomplete.getValue());
        journal.set("settlestatus", SettleStatus.noSettlement.getValue());
        journal.set("project", bizObject.get("project"));
        journal.set("refund", false);
        journal.set("bookkeeper", AppContext.getCurrentUser().getId());
        journal.set("auditinformation", "");
        journal.set("srcbillno", bizObject.get("code"));
        journal.set("srcbillitemid", bizObject.get("id"));
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
        journal.set("datacontent", DataContent.DailyData.getValue());
        //journal.set("paymentstatus",null);
        journal.setCaobject(bizObject.get("caobject"));
        journal.setBilltype(EventType.CurrencyExchangeBill);
        journal.setSrcitem(EventSource.Cmpchase);
        // 外币兑换单
        journal.setTopbilltype(EventType.CurrencyExchangeBill);
        journal.setTopsrcitem(EventSource.Cmpchase);
        journal.setBillno(billno);
        journal.setServicecode("ficmp0040");
//		journal.setTargeturl(serverUrl+"/meta/ArchiveList/"+billContext.getBillnum());
        return journal;
    }

    @Override
    public CtmJSONObject updateCurrDataAndGeneratorVoucher(CtmJSONObject jsonObject) throws Exception {
        log.error("updateCurrDataAndGeneratorVoucher params!, data={}", jsonObject);
        CtmJSONObject result = new CtmJSONObject();
        CtmJSONArray array = jsonObject.getJSONArray("data");
        for (Object o : array) {
            CtmJSONObject obj = new CtmJSONObject((Map<String, Object>) o);
            String id = obj.getString("id");
            // 状态校验处理中、已交割、逾期、交割失败
            if (obj.getShort("settlestatus") != DeliveryStatus.alreadyDelivery.getValue() && obj.getShort("settlestatus") != DeliveryStatus.beOverdueDelivery.getValue()
                    && obj.getShort("settlestatus") != DeliveryStatus.failDelivery.getValue() && obj.getShort("settlestatus") != DeliveryStatus.doingDelivery.getValue()) {
                throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418005E", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005B9", "参数错误！") /* "参数错误！" */) /* "参数错误！" */ + "settlestatus");
            }
            // 回传交割状态为已交割时，必传交割日期；其他状态，非必填；
            if (obj.getShort("settlestatus") == DeliveryStatus.alreadyDelivery.getValue() && (Objects.isNull(obj.get("settledate")) || (Objects.nonNull(obj.get("settledate")) && obj.get("settledate").toString().trim().length() == 0))) {
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22CA276E04500007", "交割日期不能为空！") /* "交割日期不能为空！" */);
            }
            CurrencyExchange currencyExchange = MetaDaoHelper.findById(CurrencyExchange.ENTITY_NAME, id);
            if (Objects.isNull(currencyExchange)) {
                throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418029F", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005BA", "单据不存在 id:") /* "单据不存在 id:" */) /* "单据不存在 id:" */ + id);
            }
            if (currencyExchange.getSettlestatus() == DeliveryStatus.alreadyDelivery || currencyExchange.getSettlestatus() == DeliveryStatus.completeDelivery) {
                throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_212E9FEA04680003", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005BB", "已经交割完成的单据，不允许更新结果信息") /* "已经交割完成的单据，不允许更新结果信息" */) /* "已经交割完成的单据，不允许更新结果信息" */ + id);
            }
            if (currencyExchange.getVerifystate() != VerifyState.COMPLETED.getValue()) {
                throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2158ED3405800001", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005BD", "单据尚未审批通过，不允许更新结果信息！") /* "单据尚未审批通过，不允许更新结果信息！" */) /* "单据尚未审批通过，不允许更新结果信息！" */ + id);
            }
            if (!Objects.isNull(obj.get("exchangerate"))) {
                BigDecimal exchangerate = new BigDecimal(obj.getString("exchangerate"));
                currencyExchange.setExchangerate(exchangerate);
            }
            if (!Objects.isNull(obj.get("sellamount"))) {
                if (new BigDecimal(obj.getString("sellamount")).compareTo(BigDecimal.ZERO) < 0) {
                    throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180432", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005AB", "金额必须大于0！") /* "金额必须大于0！" */) /* "金额必须大于0！" */);
                }
                currencyExchange.setSellamount(new BigDecimal(obj.getString("sellamount")));
            }
            if (!Objects.isNull(obj.get("purchaseamount"))) {
                if (new BigDecimal(obj.getString("purchaseamount")).compareTo(BigDecimal.ZERO) < 0) {
                    throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180432", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005AB", "金额必须大于0！") /* "金额必须大于0！" */) /* "金额必须大于0！" */);
                }
                currencyExchange.setPurchaseamount(new BigDecimal(obj.getString("purchaseamount")));
            }
            if (!Objects.isNull(obj.get("contractNo"))) {
                currencyExchange.setContractNo(obj.getString("contractNo"));
            }
            if (!Objects.isNull(obj.get("characterDef"))) {
                //特征
                BizObject bizObject = Objectlizer.convert(obj.getObject("characterDef", LinkedHashMap.class), CurrencyExchangeCharacterDef.ENTITY_NAME);
                //设置特征状态
                //原来没有值的时候，新增
                if (currencyExchange.get("characterDef") == null) {
                    bizObject.setId(String.valueOf(ymsOidGenerator.nextId()));
                    bizObject.setEntityStatus(EntityStatus.Insert);
                } else {
                    bizObject.setEntityStatus(EntityStatus.Update);
                }
                currencyExchange.put("characterDef", bizObject);
            }
            currencyExchange.setSettlestatus(DeliveryStatus.find(obj.getShort("settlestatus")));
            if (Objects.nonNull(obj.get("settledate"))) {
                currencyExchange.setSettledate(DateUtils.convertToDate(obj.getString("settledate"), DateUtils.DATE_TIME_PATTERN));
            }
            if (DeliveryStatus.alreadyDelivery.getValue() == obj.getShort("settlestatus")) {
                alreadyDeliveryUpdate(currencyExchange, currencyExchange.getExchangerate());
                if (currencyExchange.getBilltype() != null && currencyExchange.getBilltype() == EventType.CurrencyExchangeApply.getValue()) {
                    // 外币兑换申请，事项类型的单据，需更新外币申请单据状态
                    currencyApplyService.updateDeliveryStatus(currencyExchange.getCurrencyapplyid(), DeliveryStatus.alreadyDelivery.getValue(), new Date());
                }
            } else {
                currencyExchange.set("_entityName", CurrencyExchange.ENTITY_NAME);
                currencyExchange.setEntityStatus(EntityStatus.Update);
                MetaDaoHelper.update(CurrencyExchange.ENTITY_NAME, currencyExchange);
            }
        }
        result.put("code", 200);
        result.put("data", array);
        result.put("message", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054005B7", "操作成功！") /* "操作成功！" */);
        return result;
    }
}
