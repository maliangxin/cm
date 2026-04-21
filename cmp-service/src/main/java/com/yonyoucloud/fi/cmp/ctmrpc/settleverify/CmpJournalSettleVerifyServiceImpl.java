package com.yonyoucloud.fi.cmp.ctmrpc.settleverify;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.BusinessException;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.cmp.api.settleverify.CmpJournalSettleVerifyService;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.cmpentity.PaymentStatus;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.journal.JournalVo;
import com.yonyoucloud.fi.cmp.openjournal.JournalOpenCommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;
import com.yonyou.yonbip.ctm.settle.itf.param.SettleBody;
import com.yonyou.yonbip.ctm.settle.itf.param.SettleOperContext;
import com.yonyou.yonbip.ctm.settle.itf.param.SettleOperResult;
import com.yonyou.yonbip.ctm.settle.itf.param.SettleOperType;
import com.yonyou.yonbip.ctm.settle.itf.param.Settlement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <h1>日记账结算检查接口</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2024-06-29 11:28
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CmpJournalSettleVerifyServiceImpl implements CmpJournalSettleVerifyService {
    private final CTMCMPBusinessLogService ctmcmpBusinessLogService;
    private final JournalOpenCommonService journalForeginCommonService;

    @Override
    public SettleOperResult[] validate(SettleOperType operType, SettleOperContext context) throws BusinessException {
        log.error("1.CmpJournalSettleVerifyServiceImpl#validate, operType={}, context={}", operType, context);
        List<SettleOperResult> settleOperatorResultList = new ArrayList<>();
        Settlement settlement = context.getSettlement();
        if (settlement != null && settlement.getBodys() != null) {
            SettleBody[] settlementBody = settlement.getBodys();
            Set<String> settleIds = new HashSet<>();
            for (SettleBody settleBody : settlementBody) {
                String id = settleBody.getId();
                settleIds.add(id);
            }
            if (CollectionUtils.isNotEmpty(settleIds)) {
                QuerySchema querySchema = QuerySchema.create().addSelect("id, checkflag, srcbillitemid");
                QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
                queryConditionGroup.addCondition(QueryCondition.name("srcbillitemid").in(settleIds));
                querySchema.addCondition(queryConditionGroup);
                try {
                    List<Map<String, Object>> result = MetaDaoHelper.query(Journal.ENTITY_NAME, querySchema);
                    log.error("2.CmpJournalSettleVerifyServiceImpl#validate, operType={}, context={},result={}", operType, context,result);
                    if (CollectionUtils.isEmpty(result)) {
                        for (SettleBody body : settlementBody) {
                            SettleOperResult settleOperResult = new SettleOperResult(settlement.getId(), body.getId());
                            settleOperResult.setPass(Boolean.TRUE);
                            settleOperatorResultList.add(settleOperResult);
                        }
                        return settleOperatorResultList.toArray(new SettleOperResult[0]);
                    }
                    for (Map<String, Object> resultMap : result) {
                        SettleOperResult settleOperResult = new SettleOperResult(settlement.getId(), resultMap.get("srcbillitemid").toString());
                        boolean checkFlag = Boolean.parseBoolean(resultMap.get("checkflag").toString());
                        if (checkFlag) {
                            settleOperResult.setPass(false);
                            String errorMsg = String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CB5B0280438002F", "来源于结算单【%s】生成的日记账已完成对账，不允许取消结算，请检查！") /* "来源于结算单【%s】生成的日记账已完成对账，不允许取消结算，请检查！" */, settlement.getCode());
                            settleOperResult.setErrorMessage(errorMsg);
                        } else {
                            settleOperResult.setPass(true);
                        }
                        settleOperatorResultList.add(settleOperResult);
                    }
                    log.error("3.CmpJournalSettleVerifyServiceImpl#validate, operType={}, context={},settleOperatorResultList={}", operType, context,settleOperatorResultList);
                    return settleOperatorResultList.toArray(new SettleOperResult[0]);
                } catch (Exception e) {
                    log.error("4.CmpJournalSettleVerifyServiceImpl validate operType={}, context={}, errorMsg={}", operType, context, e.getMessage());
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100072"),e.getMessage());
                }
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100073"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C819AA604800062", "待检查的结算单数据为空!") /* "待检查的结算单数据为空!" */);
        }
        return new SettleOperResult[0];
    }

    @Override
    public void handle(SettleOperType operType, SettleOperContext context) throws Exception {
        log.error("1.CmpJournalSettleVerifyServiceImpl#handle, operType={}, context={}", operType, context);
        SettleOperResult[] validate = validate(operType, context);
        for (SettleOperResult settleOperResult : validate) {
            if (!settleOperResult.isPass())
                throw new Exception(settleOperResult.getErrorMessage());
        }

        Settlement settlement = context.getSettlement();
        if (settlement != null && settlement.getBodys() != null) {
            SettleBody[] settlementBody = settlement.getBodys();
            Set<String> settleIds = new HashSet<>();
            for (SettleBody settleBody : settlementBody) {
                String id = settleBody.getId();
                settleIds.add(id);
            }
            if (CollectionUtils.isNotEmpty(settleIds)) {
                QuerySchema querySchema = QuerySchema.create().addSelect("*");
                QueryConditionGroup queryConditionGroup = new QueryConditionGroup();
                queryConditionGroup.addCondition(QueryCondition.name("srcbillitemid").in(settleIds));
                querySchema.addCondition(queryConditionGroup);

                try {
                    List<Map<String, Object>> result = MetaDaoHelper.query(Journal.ENTITY_NAME, querySchema);
                    log.error("2.CmpJournalSettleVerifyServiceImpl#handle, operType={}, context={},result={}", operType, context,result);
                    String rollBackData = CtmJSONObject.toJSONString(result);
                    if (CollectionUtils.isNotEmpty(result)) {
                        List<Journal> journalList = new ArrayList<>();
                        for (Map<String, Object> journal : result) {
                            journal.put("paymentstatus", PaymentStatus.NoPay.getValue());
                            journal.put("settlestatus", SettleStatus.noSettlement.getValue());
                            journal.put("auditstatus", AuditStatus.Complete.getValue());
                            journal.put("dzdate", null);
                            Journal journalVo = new Journal();
                            journalVo.init(journal);
                            journalList.add(journalVo);
                        }
                        JournalVo journalVo = new JournalVo();
                        journalVo.setUniqueIdentification(settlement.getId() + settlement.getPk_accentity());
                        journalVo.setJournalList(journalList);
                        log.error("3.CmpJournalSettleVerifyServiceImpl#handle, operType={}, context={},journalVo={}", operType, context,journalVo);
//                        if (SettleOperType.CANCE_SETTLE.equals(operType)) {
//                            journalForeginCommonService.journalUpdate(journalVo);
//                        } else
                        if (SettleOperType.BACK_SETTLEPOOL.equals(operType)) {
                            journalForeginCommonService.journalDelete(journalVo);
                        }
                        // 业务日志
                        CtmJSONObject logData = new CtmJSONObject();
                        logData.put("params", "tempAccentity:"+journalVo.getTempAccentity()+", tempBilltype:"+journalVo.getTempBilltype());
                        logData.put("hcsum", journalVo.getHcsum());
                        logData.put("uniqueIdentification", journalVo.getUniqueIdentification().toString());
                        logData.put("journalList", journalVo.getJournalList().toString());
                        String customNo = "CmpJournalSettleVerifyServiceImpl";
                        ctmcmpBusinessLogService.saveBusinessLog(logData, customNo, customNo, null,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400791", "日记账结算") /* "日记账结算" */, com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400790", "日记账结算检查") /* "日记账结算检查" */);

                        List<Object> bizObjectList =CtmJSONObject.parseObject(rollBackData, List.class);
                        YtsContext.setYtsContext("UPDATE_JOURNAL_HANDLE", bizObjectList);
                        log.error("4.CmpJournalSettleVerifyServiceImpl#handle, operType={}, context={},bizObjectList={}", operType, context,bizObjectList);
                    }
                } catch (Exception e) {
                    log.error("5.CmpJournalSettleVerifyServiceImpl handle errorMsg={}", e.getMessage());
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100074"),e.getMessage());
                }
            }

        }
    }

    @Override
    public Object rollBackJournalHandle(SettleOperType operType, SettleOperContext context) throws Exception {
        try {
            Settlement settlement = context.getSettlement();
            List<Object> bizObjectList = (List<Object>) YtsContext.getYtsContext("UPDATE_JOURNAL_HANDLE");
            log.error("1.CmpJournalSettleVerifyServiceImpl#rollBackJournalHandle, operType={}, context={}, bizObjectList={}",
                    operType, context, bizObjectList);
            if (CollectionUtils.isNotEmpty(bizObjectList)) {
                List<Journal> journalList = new ArrayList<>();
                for (Object biz : bizObjectList) {
                    Map<String, Object> bizObject = (Map<String, Object>) biz;
                    Journal journalVo = new Journal();
                    journalVo.init(bizObject);
                    journalList.add(journalVo);
                }
                JournalVo journalVo = new JournalVo();
                journalVo.setUniqueIdentification(settlement.getId() + settlement.getPk_accentity());
                journalVo.setJournalList(journalList);
                log.error("2.CmpJournalSettleVerifyServiceImpl#rollBackJournalHandle, journalVo={}", journalVo);
//                if (SettleOperType.CANCE_SETTLE.equals(operType)) {
//                    journalForeginCommonService.rollbackJournalUpdate(journalVo);
//                } else
                if (SettleOperType.BACK_SETTLEPOOL.equals(operType)) {
                    journalForeginCommonService.rollbackJournalDelete(journalVo);
                }

                Map<String, Object> resultResponse = new HashMap<>();
                resultResponse.put("code", 200);
                resultResponse.put("isSuccess", true);
                return resultResponse;
            }
        } catch (Exception e) {
            log.error("2.CmpJournalSettleVerifyServiceImpl#rollBackJournalHandle, operType={}, context={}, errorMsg={}",
                    operType, context, e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100075"),e.getMessage());
        }
        return new RuleExecuteResult(999, "no data");
    }
}
