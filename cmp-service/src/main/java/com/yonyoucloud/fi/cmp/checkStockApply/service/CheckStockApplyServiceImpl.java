package com.yonyoucloud.fi.cmp.checkStockApply.service;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.lock.CtmLockTool;
import com.yonyou.yonbip.ctm.util.lock.LockStatus;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.checkStock.service.CheckStatusService;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.checkstockapply.CheckStockApply;
import com.yonyoucloud.fi.cmp.checkstockapply.CmpBusiType;
import com.yonyoucloud.fi.cmp.checkstockapply.RecCheckTemp;
import com.yonyoucloud.fi.cmp.cmpentity.AuditStatus;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.enums.YesOrNoEnum;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckDir;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpLock;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 支票入库，服务类
 */
@Service
@Transactional(rollbackFor = RuntimeException.class)
@Slf4j
public class CheckStockApplyServiceImpl implements CheckStockApplyService {

    public static final String auditLockControl = "cmp_checkStockApply_audit";
    public static final String unAuditLockControl = "cmp_checkStockApply_unAudit";
    @Autowired
    private CmCommonService cmCommonService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    private BaseRefRpcService baseRefRpcService;
    @Autowired
    CheckStatusService checkStatusService;

    /**
     * 支票入库，审批
     *
     * @param rows
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONArray insertCheckStockService(CtmJSONArray rows) throws Exception {
        if (null == rows || rows.size() < 1) {
            //没有可操作的数据！
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100721"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800E1", "请选择单据！") /* "请选择单据！" */);
        }
        List<Object> ids = getIdsByRows(rows);
        List<CheckStockApply> checkStorkApplyRows = getCheckStorkApplyById(ids);
        checkInsertAudits(checkStorkApplyRows);
        List<CheckStock> checkStocks = buildCheckStorkByApply(checkStorkApplyRows);
        checkCheckStocks(checkStocks);
        List<CheckStockApply> updateRow = getUpdateApplyList(checkStorkApplyRows, AuditStatus.Complete, VerifyState.COMPLETED);
        checkStatusService.recordCheckStatusByInsert(checkStocks);
        CmpMetaDaoHelper.insert(CheckStock.ENTITY_NAME, checkStocks);
        MetaDaoHelper.update(CheckStockApply.ENTITY_NAME, updateRow);
        cmCommonService.refreshPubTs(CheckStockApply.ENTITY_NAME, ids, rows);
        return rows;
    }

    /**
     * 支票入库，审核弃审，批量修改审批状态
     *
     * @param checkStorkApplyRows
     * @param status
     * @return
     */
    private List<CheckStockApply> getUpdateApplyList(List<CheckStockApply> checkStorkApplyRows, AuditStatus status, VerifyState verifyState) {
        if (null == checkStorkApplyRows || checkStorkApplyRows.size() < 0) {
            return null;
        }
        List<CheckStockApply> updateList = new ArrayList<>();
        for (CheckStockApply apply : checkStorkApplyRows) {
            CheckStockApply update = new CheckStockApply();
            update.setEntityStatus(EntityStatus.Update);
            update.setId(apply.getId());
            update.setAuditstatus(status.getValue());
            update.setVerifystate(verifyState.getValue());
            // 审核、弃审、设置审批人，审批时间
            if (AuditStatus.Complete == status) {// 审批
                update.setAuditorId(AppContext.getCurrentUser().getId());
                update.setAuditor(AppContext.getCurrentUser().getName());
                update.setAuditDate(BillInfoUtils.getBusinessDate());
                update.setAuditTime(BillInfoUtils.getBusinessDate());
            } else {
                update.setAuditorId(null);
                update.setAuditor(null);
                update.setAuditDate(null);
                update.setAuditTime(null);
            }
            // 设置审批人，审批时间

            updateList.add(update);
        }
        return updateList;
    }

    /**
     * 支票入库，弃审
     *
     * @param rows
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONArray abandonCheckStock(CtmJSONArray rows) throws Exception {
        if (null == rows || rows.size() < 1) {
            //没有可操作的数据！
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100721"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800E1", "请选择单据！") /* "请选择单据！" */);
        }
        List<Object> ids = getIdsByRows(rows);
        List<CheckStockApply> checkStorkApplyRows = getCheckStorkApplyById(ids);
        checkAbandonAudits(checkStorkApplyRows);
        abandonCheckStocks(checkStorkApplyRows);
        cmCommonService.refreshPubTs(CheckStockApply.ENTITY_NAME, ids, rows);
        return rows;
    }

    /**
     * 支票预占
     *
     * @param param
     * @return
     */
    @Override
    public void checkStockOccupy(CtmJSONObject param) throws Exception {
        log.error("接口入参 param:" + CtmJSONObject.toJSONString(param));
        String newCheckId = param.getString("newCheckId");
        if (ValueUtils.isNotEmptyObj(newCheckId)) {
            CtmJSONObject newCheckIdParam = new CtmJSONObject();
            newCheckIdParam.put("newCheckId", newCheckId);
            try {
                CtmLockTool.executeInOneServiceLock("OCCUPY_CHECK_BUSINESS" + newCheckId, 60 * 60 * 2L, TimeUnit.SECONDS, (int lockstatus) -> {
                    if (lockstatus == LockStatus.GETLOCK_FAIL) {
                        //加锁失败
                        return;
                    }
                    //加锁成功
                    updateCheckStock(newCheckIdParam);
                });
            } catch (Exception e) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100722"), e.getMessage());
            }
        }
        if (ValueUtils.isNotEmptyObj(param.get("oldCheckId"))) {
            CtmJSONObject oldCheckIdParam = new CtmJSONObject();
            if (param.get("oldCheckId") instanceof List) {
                oldCheckIdParam.put("oldCheckId", param.get("oldCheckId"));
            }else{
                oldCheckIdParam.put("oldCheckId",param.getString("oldCheckId"));
            }

            updateCheckStock(oldCheckIdParam);
        }
    }

    private void updateCheckStock(CtmJSONObject param) throws Exception {
        /** 获取字段属性 */
        List<String> newCheckStocks = new ArrayList<>();
        List<String> oldCheckStocks = new ArrayList<>();
        Object oldCheckId = param.get("oldCheckId");//旧支票id
        Object newCheckId = param.get("newCheckId");//新支票id
        if (ObjectUtils.isNotEmpty(oldCheckId)) {
            if (oldCheckId instanceof String) {
                oldCheckId = ((String) oldCheckId).replace("[", "").replace("]", "");//新支票id
                oldCheckStocks.add((String) oldCheckId);
            } else if (oldCheckId instanceof List) {
                oldCheckStocks = (List<String>) oldCheckId;
            }
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").in(oldCheckStocks));
            querySchema.addCondition(group);
            List<CheckStock> checkStockList = MetaDaoHelper.queryObject(CheckStock.ENTITY_NAME, querySchema, null);
            checkStockList.stream().forEach(e -> {
                e.setEntityStatus(EntityStatus.Update);
                e.setOccupy((short) 0);
            });
            MetaDaoHelper.update(CheckStock.ENTITY_NAME, checkStockList);
        }
        if (ObjectUtils.isNotEmpty(newCheckId)) {
            if (newCheckId instanceof String) {
                newCheckId = ((String) newCheckId).replace("[", "").replace("]", "");//新支票id
                // 去除空格和方括号，然后按逗号分割
                String cleanedNewCheckId = ((String) newCheckId).replaceAll("[\\[\\]\\s]", "");
                newCheckStocks.addAll(Arrays.asList(cleanedNewCheckId.split(",")));

            } else if (oldCheckId instanceof List) {
                newCheckStocks = (List<String>) newCheckId;
            }
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").in(newCheckStocks));
            querySchema.addCondition(group);
            List<CheckStock> checkStockList = MetaDaoHelper.queryObject(CheckStock.ENTITY_NAME, querySchema, null);
            checkStockList.stream().forEach(e -> {
                e.setEntityStatus(EntityStatus.Update);
                e.setOccupy((short) 1);
            });
            MetaDaoHelper.update(CheckStock.ENTITY_NAME, checkStockList);
        }
    }


    /**
     * 弃审校验
     *
     * @param checkStorkApplyRows
     * @throws Exception
     */
    private void abandonCheckStocks(List<CheckStockApply> checkStorkApplyRows) throws Exception {
        List<Long> ids = new ArrayList<>();
        checkStorkApplyRows.forEach(apply -> {
            ids.add(apply.getId());
        });
        List<CheckStock> cpmCheckStocks = getCpmCheckStocks(ids);
        for (CheckStock stock : cpmCheckStocks) {
            if (!(CmpCheckStatus.InStock.getValue().equals(stock.getCheckBillStatus()) && CmpLock.NO.getValue().equals(stock.getIsLock()))) {
                //支票已被操作，不能弃审
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100723"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800E0", "支票已被操作，不能弃审") /* "支票已被操作，不能弃审" */);
            }
        }
        List<CheckStockApply> updateRow = getUpdateApplyList(checkStorkApplyRows, AuditStatus.Incomplete, VerifyState.INIT_NEW_OPEN);
        for (int i = 0; i < cpmCheckStocks.size(); i++) {
            Long id = cpmCheckStocks.get(i).getId();
            MetaDaoHelper.deleteByObjectId(CheckStock.ENTITY_NAME, id);
        }
        MetaDaoHelper.update(CheckStockApply.ENTITY_NAME, updateRow);
    }

    private void checkAbandonAudits(List<CheckStockApply> checkStorkApplyRows) throws Exception {
        for (CheckStockApply apply : checkStorkApplyRows) {
            if (AuditStatus.Incomplete.getValue() == apply.getAuditstatus()) {
                //存在未审批的数据，不能弃审
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100724"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800DF", "存在未审批的数据，不能弃审") /* "存在未审批的数据，不能弃审" */);
            }
        }
        List<Long> ids = new ArrayList<>();
        checkStorkApplyRows.forEach(apply -> {
            ids.add(apply.getId());
        });
        List<CheckStock> cpmCheckStocks = getCpmCheckStocks(ids);
        for (CheckStock stock : cpmCheckStocks) {
            if (!(CmpCheckStatus.InStock.getValue().equals(stock.getCheckBillStatus()) && CmpLock.NO.getValue().equals(stock.getIsLock()))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100720"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800C1", "存在已使用/处置的支票，不允许撤回") /* "存在已使用/处置的支票，不允许撤回" */);
            }
            if (stock.getOccupy() != null && stock.getOccupy() == YesOrNoEnum.YES.getValue()) {
                // 若支票编号已被预占，则提示失败
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2290728804B00000", "支票编号已被预占，不能撤回！"));
            }
        }
    }

    /**
     * 审核校验 - 支票编号重复
     *
     * @param checkStocks
     * @throws Exception
     */
    private void checkCheckStocks(List<CheckStock> checkStocks) throws Exception {
        List<String> checkBillNoList = new ArrayList<>();
        checkStocks.forEach(stock -> checkBillNoList.add(stock.getCheckBillNo()));
        HashSet<String> hashSet = new HashSet<>(checkBillNoList);
        if (checkBillNoList.size() != hashSet.size()) {
            //存在相同的支票编号！
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100725"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800DE", "审核失败：存在相同的支票编号！") /* "审核失败：存在相同的支票编号！" */);
        }
        List<CheckStock> checkNos = getCheckNos();
        for (CheckStock s : checkNos) {
            for (String no : checkBillNoList) {
                if (s.getCheckBillNo().equals(no)) {
                    //存在相同的支票编号！
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100725"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800DE", "审核失败：存在相同的支票编号！") /* "审核失败：存在相同的支票编号！" */);
                }
            }
        }
    }

    /**
     * 根据“支票入库”单据，生成“支票工作台”单据
     *
     * @param checkStorkApplyRows
     * @return
     * @throws Exception
     */
    private List<CheckStock> buildCheckStorkByApply(List<CheckStockApply> checkStorkApplyRows) throws Exception {
        List<CheckStock> stockList = new ArrayList<>();
        for (CheckStockApply apply : checkStorkApplyRows) {
            short chequeType = apply.getChequeType();
            List<CheckStock> stocks = null;
            if (CmpBusiType.Black.getValue() == chequeType) {
                stocks = buildBlack(apply);
            } else {
                stocks = buildRec(apply);
            }
            stockList.addAll(stocks);
        }
        return stockList;
    }

    /**
     * 生成收票单据
     *
     * @param apply
     * @return
     * @throws Exception
     */
    private List<CheckStock> buildRec(CheckStockApply apply) throws Exception {
        Object id = apply.getId();
        List<RecCheckTemp> tempList = getCheckTempById(id);
        List<CheckStock> stockList = new ArrayList<>();
        for (RecCheckTemp temp : tempList) {
            CheckStock stock = new CheckStock();
            stock.setId(ymsOidGenerator.nextId());
            stock.setAccentity(apply.getAccentity());
            stock.setBillType(apply.getBillType());
            stock.setBusiDate(apply.getVouchdate());//收入支票，生成支票库存，支票工作台入库日期取支票入库主表的入库日期
            stock.setCheckBillDir(CmpCheckDir.Com.getValue());
            stock.setCheckBillStatus(CmpCheckStatus.InStock.getValue());
            stock.setCheckBillType(temp.getCheckBillType());
            stock.setCreateDate(new Date());
            stock.setCreater(apply.getCreator());
            stock.setCreatorId(apply.getCreatorId());
            stock.setCurrency(apply.getCurrency());
            stock.setCustNo(apply.getOrg());
            stock.setPayBank(temp.getPayBank());
            stock.setAmount(temp.getAmount());
            stock.setCheckBillNo(temp.getCheckBillNo());
            stock.setDrawerDate(temp.getDrawerDate());
            stock.setDrawerAcctNo(temp.getDrawerAcctNo());
            stock.setPayBank(temp.getPayBank());
            List<Map<String, Object>> orgMVById = QueryBaseDocUtils.getOrgMVById(apply.getOrg());
            if (null != orgMVById && orgMVById.size() > 0) {
                stock.setPayeeName((String) orgMVById.get(0).get("name"));
            }
            stock.setEnableEndorse(temp.getEnableEndorse());
            stock.setMainid(temp.get("id"));
            stock.setInputBillNo(apply.getCode());
            stock.setIsLock("0");
            stock.setCheckBillStatus(CmpCheckStatus.InStock.getValue());
            stock.setMainid(apply.getId());
            stockList.add(stock);
        }
        return stockList;
    }

    /**
     * 支票编号计算
     *
     * @param apply
     * @param i
     * @return
     */
    private String getCheckBillNoByNum(CheckStockApply apply, int i) {
        String startNo = apply.getStartNo();
        String[] split = startNo.replaceAll("\\D", ",").split(",");
        String end = split[split.length - 1];
        BigInteger num = new BigInteger(end);
        BigInteger iBig = new BigInteger(String.valueOf(i));
        String val = String.valueOf(num.add(iBig));
        if (startNo.length() >= val.length()) {
            String begin = startNo.substring(0, startNo.indexOf(String.valueOf(num)));
            return begin + val;
        } else {
            return val;
        }
    }

    /**
     * 生成空白支票
     *
     * @param apply
     * @return
     * @throws Exception
     */
    private List<CheckStock> buildBlack(CheckStockApply apply) throws Exception {
        List<CheckStock> stockList = new ArrayList<>();
        Integer stockNum = apply.getStockNum();
        for (int i = 0; i < stockNum; i++) {
            CheckStock stock = new CheckStock();
            stock.setId(ymsOidGenerator.nextId());
            stock.setAccentity(apply.getAccentity());
            stock.setBillType(apply.getBillType());
            stock.setBusiDate(apply.getVouchdate());
            stock.setCheckBillDir(CmpCheckDir.Cash.getValue());
            stock.setCheckBillStatus(CmpCheckStatus.InStock.getValue());
            stock.setCheckBillType(apply.getCheckBillType());
            stock.setCheckBookNo(apply.getCheckBookNo());
            stock.setCreateDate(new Date());
            stock.setCreater(apply.getCreator());
            stock.setCreatorId(apply.getCreatorId());
            stock.setCurrency(apply.getCurrency());
            EnterpriseBankAcctVO stringObjectMap = baseRefRpcService.queryEnterpriseBankAccountById(apply.getAccount());
            stock.setCustNo(apply.getOrg());
            stock.setPayBank(stringObjectMap.getBankNumber());
            stock.setInputBillNo(apply.getCode());
            stock.setIsLock("0");
            stock.setCheckBillStatus(CmpCheckStatus.InStock.getValue());
            stock.setDrawerAcct(apply.getAccount());
            stock.setDrawerAcctNo(stringObjectMap.getAccount());
            stock.setDrawerAcctName(stringObjectMap.getName());
            String checkBillNo = getCheckBillNoByNum(apply, i);
            stock.setCheckBillNo(checkBillNo);
            stock.setMainid(apply.getId());
            stockList.add(stock);
        }
        return stockList;
    }

    /**
     * 审核校验 - 重复审批
     *
     * @param checkStorkApplyRows
     */
    private void checkInsertAudits(List<CheckStockApply> checkStorkApplyRows) {
        checkStorkApplyRows.forEach(apply -> {
            Short auditstatus = apply.getAuditstatus();
            if (AuditStatus.Complete.getValue() == auditstatus) {
                //存在已被审批的数据，不能再次审批
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100726"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800E2", "失败：存在已被审批的数据，不能再次审批！") /* "失败：存在已被审批的数据，不能再次审批！" */);
            }
        });
    }

    /**
     * 获取主键id
     *
     * @param rows
     * @return
     */
    private List<Object> getIdsByRows(CtmJSONArray rows) {
        if (null == rows || rows.size() < 1) {
            return null;
        }
        List<Object> ids = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject checkAll = rows.getJSONObject(i);
            Long id = checkAll.getLong("id");
            ids.add(id);
        }
        return ids;
    }

    /**
     * 根据id查询“支票入库”单据最新状态
     *
     * @param ids
     * @return
     * @throws Exception
     */
    private List<CheckStockApply> getCheckStorkApplyById(List<Object> ids) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        querySchema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryObject(CheckStockApply.ENTITY_NAME, querySchema, null);
    }

    /**
     * 获取“支票入库 - 收入支票”子表数据
     *
     * @param id
     * @return
     * @throws Exception
     */
    public List<RecCheckTemp> getCheckTempById(Object id) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("mainid").eq(id));
        querySchema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryObject(RecCheckTemp.ENTITY_NAME, querySchema, null);
    }

    /**
     * 查询数据库中所有状态符合的“支票工作台”单据的支票编号
     *
     * @return
     * @throws Exception
     */
    private List<CheckStock> getCheckNos() throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("checkBillStatus").in("1,2,3,4,5,6,7".split(",")));
        querySchema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryObject(CheckStock.ENTITY_NAME, querySchema, null);
    }

    /**
     * 根据主表id获取“支票工作台”单据
     *
     * @param ids
     * @return
     * @throws Exception
     */
    private List<CheckStock> getCpmCheckStocks(List<Long> ids) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("mainid").in(ids));
        querySchema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryObject(CheckStock.ENTITY_NAME, querySchema, null);
    }

}
