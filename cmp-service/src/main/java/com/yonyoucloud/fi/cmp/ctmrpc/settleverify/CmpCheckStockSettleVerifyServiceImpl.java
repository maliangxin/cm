package com.yonyoucloud.fi.cmp.ctmrpc.settleverify;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.settle.itf.param.*;
import com.yonyoucloud.fi.cmp.api.settleverify.CmpCheckStockSettleVerifyService;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStatusService;
import com.yonyoucloud.fi.cmp.checkstock.CheckStatus;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckDir;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <h1>支票结算检查接口</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2024-07-26 11:49
 */
@Service
@RequiredArgsConstructor()
@Slf4j
public class CmpCheckStockSettleVerifyServiceImpl implements CmpCheckStockSettleVerifyService {

    private final CheckStatusService checkStatusService;

    @Override
    public SettleOperResult[] validate(SettleOperType operType, SettleOperContext context) throws Exception {
        List<SettleOperResult> result = new ArrayList<>();
        Settlement settlement = context.getSettlement();
        SettleBody[] bodys = settlement.getBodys();
        for (SettleBody body : bodys) {
            SettleOperResult settleOperResult = new SettleOperResult(settlement.getId(), body.getId());
            settleOperResult.setPass(Boolean.TRUE);
            result.add(settleOperResult);
        }
        return result.toArray(new SettleOperResult[0]);
    }

    @Override
    public void handle(SettleOperType operType, SettleOperContext context) throws Exception {
        log.error("1.CmpCheckStockSettleVerifyServiceImpl#handle, operType={}, context={}", operType, context);
        SettleOperResult[] validate = validate(operType, context);
        for (SettleOperResult settleOperResult : validate) {
            if (!settleOperResult.isPass())
                throw new Exception(settleOperResult.getErrorMessage());
        }
        Settlement settlement = context.getSettlement();
        if (settlement != null && settlement.getBodys() != null) {
            SettleBody[] settlementBody = settlement.getBodys();
            Set<String> checkIds = new HashSet<>();
            Map<String, String> params = new HashMap<>();
            for (SettleBody settleBody : settlementBody) {
                String checkId = settleBody.getPk_merchantbillno();
                checkIds.add(checkId);
                params.put(checkId, settleBody.getReceiptType());
            }
            if (CollectionUtils.isEmpty(checkIds)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102577"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C819AA604800057", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C819AA604800057", "入参的数据支票id为空!") /* "入参的数据支票id为空!" */) /* "入参的数据支票id为空!" */);
            }
            // 批量查询支票数据
            List<Map<String, Object>> mapList = MetaDaoHelper.queryByIds(CheckStock.ENTITY_NAME, "*", new ArrayList<>(checkIds));

            List<CheckStock> checkStockList = new ArrayList<>();

            for (Map<String, Object> map : mapList) {
                CheckStock checkStock = new CheckStock();
                checkStock.init(map);
                checkStockList.add(checkStock);
            }
            log.error("2.CmpCheckStockSettleVerifyServiceImpl#handle, checkStockList={}", checkStockList);
            if (CollectionUtils.isEmpty(checkStockList)) {
                return;
            }
            String rollBackData = CtmJSONObject.toJSONString(checkStockList);
            // 组装事务回滚数据
            Map<String, Object> rollBackMap = new HashMap<>();

            Map<String, String> rollBackUpdateBeforeStatusMap = new HashMap<>();
            Map<String, String> rollBackUpdateAfterStatusMap = new HashMap<>();
            // 取消结算
            if (SettleOperType.CANCE_SETTLE.equals(operType)) {
                for (CheckStock checkStock : checkStockList) {
                    rollBackUpdateBeforeStatusMap.put(checkStock.getId().toString(), checkStock.getCheckBillStatus());
                    // '2':付票   '1':收票
                    Object id = checkStock.getId();
                    String receiptType = params.get(id.toString());
                    if (CmpCheckDir.Cash.getValue().equals(receiptType)) {
                        String checkBillDir = checkStock.getCheckBillDir();
                        if (CmpCheckDir.Cash.getValue().equals(checkBillDir)) {
                            // 取消结算时，支票状态应该退回“已开票”，结算单弃审时应该另外再调用checkOperation更新支票状态为“在开票”
                            checkStock.setCheckBillStatus(CmpCheckStatus.BillOver.getValue());
                        } else {
                            // 无此种情况，方向不允许，也就没有背书相关支票状态
                            checkStock.setCheckBillStatus(CmpCheckStatus.Endorsing.getValue());
                        }
                    } else {
                        checkStock.setCheckBillStatus(CmpCheckStatus.Cashing.getValue());
                    }
                    // 清空支票的兑付日期
                    checkStock.setCashDate(null);
                    // 清空支票的支票用途
                    checkStock.setCheckpurpose(null);
                    // 清空支票的兑付方式
                    checkStock.setCashType(null);
                    // 清空支票的兑付人
                    checkStock.setCashPerson(null);
                    rollBackUpdateAfterStatusMap.put(checkStock.getId().toString(), checkStock.getCheckBillStatus());
                }
            } else {
                // 退回待结算
                for (CheckStock checkStock : checkStockList) {
                    rollBackUpdateBeforeStatusMap.put(checkStock.getId().toString(), checkStock.getCheckBillStatus());
                    Object id = checkStock.getId();
                    String receiptType = params.get(id.toString());
                    if (CmpCheckDir.Cash.getValue().equals(receiptType)) {
                        QuerySchema schema = QuerySchema.create().addSelect("afterCheckBillStatus, id, pubts");
                        QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                        // 只查询来源为现金的数据 只有这类数据需要升级
                        conditionGroup.appendCondition(QueryCondition.name("checkId").eq(id),
                                QueryCondition.name("afterCheckBillStatus").in((Object) new Integer[]{1, 13}));
                        schema.addCondition(conditionGroup);
                        schema.addOrderBy(new QueryOrderby("pubts", "desc"));
                        schema.setLimitCount(1);
                        List<BizObject> bizObjects = MetaDaoHelper.queryObject(CheckStatus.ENTITY_NAME, schema, null);

                        if (!CollectionUtils.isEmpty(bizObjects)) {
                            checkStock.setCheckBillStatus(bizObjects.get(0).getString("afterCheckBillStatus"));
                        }
                        checkStock.setAmount(null);
                        checkStock.setDrawerDate(null);
                        checkStock.setDrawerName(null);
                        checkStock.setPayeeName(null);
                    } else {
                        checkStock.setCheckBillStatus(CmpCheckStatus.InStock.getValue());
                    }
                    rollBackUpdateAfterStatusMap.put(checkStock.getId().toString(), checkStock.getCheckBillStatus());
                }
            }
            rollBackMap.put("data", rollBackData);
            rollBackMap.put("rollBackUpdateBeforeStatusMap", rollBackUpdateBeforeStatusMap);
            rollBackMap.put("rollBackUpdateAfterStatusMap", rollBackUpdateAfterStatusMap);
            YtsContext.setYtsContext("UPDATE_CHECK_STOCK_HANDLE", rollBackMap);
            log.error("3.CmpCheckStockSettleVerifyServiceImpl#handle, rollBackMap={},checkStockList={}", rollBackMap, checkStockList);
            EntityTool.setUpdateStatus(checkStockList);
            MetaDaoHelper.update(CheckStock.ENTITY_NAME, checkStockList);
            // 插入支票变更记录表
            saveCheckStatusRecord(rollBackData, rollBackUpdateAfterStatusMap, checkStockList);
        }
    }

    private void saveCheckStatusRecord(String rollBackData, Map<String, String> rollBackUpdateAfterStatusMap, List<CheckStock> checkStockList) throws Exception {
        List<Object> list = CtmJSONObject.parseObject(rollBackData, List.class);
        List<CheckStatus> checkStatusList = new ArrayList<>();
        for (Object object : list) {
            CheckStock checkStock = null;
            if (object instanceof Map) {
                checkStock = new CheckStock();
                checkStock.init((Map<String, Object>) object);
            }
            if (ValueUtils.isNotEmptyObj(checkStock)) {
                Object drawerDate = checkStock.get("drawerDate");
                if (ValueUtils.isNotEmptyObj(drawerDate) && drawerDate instanceof String) {
                    Date date = DateUtils.strToDate(drawerDate.toString());
                    checkStock.setDrawerDate(date);
                }
                Object busiDate = checkStock.get("busiDate");
                if (ValueUtils.isNotEmptyObj(busiDate) && busiDate instanceof String) {
                    Date date = DateUtils.strToDate(busiDate.toString());
                    checkStock.setBusiDate(date);
                }
                Object cashDate = checkStock.get("cashDate");
                if (ValueUtils.isNotEmptyObj(cashDate) && cashDate instanceof String) {
                    Date date = DateUtils.strToDate(cashDate.toString());
                    checkStock.setCashDate(date);
                }
                Object pubts = checkStock.get("pubts");
                if (ValueUtils.isNotEmptyObj(pubts) && pubts instanceof String) {
                    Date date = DateUtils.strToDate(pubts.toString());
                    checkStock.setPubts(date);
                }
                Object createDate = checkStock.get("createDate");
                if (ValueUtils.isNotEmptyObj(createDate) && pubts instanceof String) {
                    Date date = DateUtils.strToDate(createDate.toString());
                    checkStock.setCreateDate(date);
                }
                Object amount = checkStock.get("amount");
                if (ValueUtils.isNotEmptyObj(amount)) {
                    BigDecimal bigDecimal = new BigDecimal(amount.toString());
                    checkStock.setAmount(bigDecimal);
                }
                CheckStatus checkStatus = checkStatusService.buildCheckStatus(checkStock,
                        rollBackUpdateAfterStatusMap.get(checkStock.getId().toString()),
                        null,
                        null);
                checkStatusList.add(checkStatus);
            }
            if (CollectionUtils.isNotEmpty(checkStockList)) {
                CmpMetaDaoHelper.insert(CheckStatus.ENTITY_NAME, checkStatusList);
            }
        }
    }

    @Override
    public Object rollBackCheckStockHandle(SettleOperType operType, SettleOperContext context) throws Exception {
        Map<String, Object> rollBackMap = (Map<String, Object>) YtsContext.getYtsContext("UPDATE_CHECK_STOCK_HANDLE");
        log.error("1.CmpCheckStockSettleVerifyServiceImpl#rollBackCheckStockHandle, operType={}, context={}, rollBackMap={},"
                , operType, context, rollBackMap);
        if (ValueUtils.isNotEmptyObj(rollBackMap)) {
            String rollBackData = (String) rollBackMap.get("data");

            List<Object> list = CtmJSONObject.parseObject(rollBackData, List.class);
            Set<Long> idsList = new HashSet<>();
            for (Object object : list) {
                CheckStock checkStock = null;
                if (object instanceof Map) {
                    checkStock = new CheckStock();
                    checkStock.init((Map<String, Object>) object);

                    Long id = checkStock.getId();
                    idsList.add(id);

                }
            }

            List<Map<String, Object>> mapList = MetaDaoHelper.queryByIds(CheckStock.ENTITY_NAME, "*", idsList.toArray(new Long[0]));

            Map<Long, Map<String, Object>> dbCheckStockMap = mapList.stream()
                    .collect(Collectors.toMap(item -> Long.parseLong(item.get("id").toString()), item -> item));

            List<CheckStock> checkStockList= new ArrayList<>();
            for (Object object : list) {
                if (object instanceof Map) {
                    CheckStock checkStockBeforeUpdate = new CheckStock();
                    checkStockBeforeUpdate.init((Map<String, Object>) object);

                    Object drawerDate = checkStockBeforeUpdate.get("drawerDate");
                    if (ValueUtils.isNotEmptyObj(drawerDate) && drawerDate instanceof String) {
                        Date date = DateUtils.strToDate(drawerDate.toString());
                        checkStockBeforeUpdate.setDrawerDate(date);
                    }
                    Object cashDate = checkStockBeforeUpdate.get("cashDate");
                    if (ValueUtils.isNotEmptyObj(cashDate) && cashDate instanceof String) {
                        Date date = DateUtils.strToDate(cashDate.toString());
                        checkStockBeforeUpdate.setCashDate(date);
                    }
                    Object amount = checkStockBeforeUpdate.get("amount");
                    if (ValueUtils.isNotEmptyObj(amount)) {
                        BigDecimal bigDecimal = new BigDecimal(amount.toString());
                        checkStockBeforeUpdate.setAmount(bigDecimal);
                    }

                    Long id = checkStockBeforeUpdate.getId();
                    Map<String, Object> objectMap = dbCheckStockMap.get(id);
                    CheckStock checkStock = new CheckStock();
                    checkStock.init(objectMap);
                    checkStock.setCashDate(checkStockBeforeUpdate.getCashDate());
                    checkStock.setCheckpurpose(checkStockBeforeUpdate.getCheckpurpose());
                    checkStock.setAmount(checkStockBeforeUpdate.getAmount());
                    checkStock.setDrawerDate(checkStockBeforeUpdate.getDrawerDate());
                    checkStock.setDrawerName(checkStockBeforeUpdate.getDrawerName());
                    checkStock.setPayeeName(checkStockBeforeUpdate.getPayeeName());
                    checkStock.setCheckBillStatus(checkStockBeforeUpdate.getCheckBillStatus());
                    if (ValueUtils.isNotEmptyObj(checkStock)) {
                        checkStockList.add(checkStock);
                    }
                }
            }
            log.error("3.CmpCheckStockSettleVerifyServiceImpl#rollBackCheckStockHandle, checkStockList={}", checkStockList);
            if (CollectionUtils.isNotEmpty(checkStockList)) {
                EntityTool.setUpdateStatus(checkStockList);
                MetaDaoHelper.update(CheckStock.ENTITY_NAME, checkStockList);
            }

            // 插入支票变更记录表
            if (CollectionUtils.isNotEmpty(checkStockList)) {
                Map<String, String> rollBackUpdateAfterStatusMap = (Map<String, String>) rollBackMap.get("rollBackUpdateAfterStatusMap");
                Map<String, String> rollBackUpdateBeforeStatusMap = (Map<String, String>) rollBackMap.get("rollBackUpdateBeforeStatusMap");
                log.error("3.CmpCheckStockSettleVerifyServiceImpl#rollBackCheckStockHandle, rollBackUpdateAfterStatusMap={}, rollBackUpdateBeforeStatusMap={}",
                        rollBackUpdateAfterStatusMap, rollBackUpdateBeforeStatusMap);
                List<CheckStatus> checkStatusList = new ArrayList<>();
                for (CheckStock checkStock : checkStockList) {
                    checkStock.setCheckBillStatus(rollBackUpdateAfterStatusMap.get(checkStock.getId().toString()));
                    CheckStatus checkStatus = checkStatusService.buildCheckStatus(checkStock, rollBackUpdateBeforeStatusMap.get(checkStock.getId().toString()), null, null);
                    checkStatusList.add(checkStatus);
                }
                CmpMetaDaoHelper.insert(CheckStatus.ENTITY_NAME, checkStatusList);
            }
        }
        Map<String, Object> resultResponse = new HashMap<>();
        resultResponse.put("code", 200);
        resultResponse.put("isSuccess", true);
        return resultResponse;
    }
}
