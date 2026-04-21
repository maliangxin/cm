package com.yonyoucloud.fi.cmp.checkStock.service;

import com.google.common.collect.Lists;
import com.yonyou.diwork.ott.exexutors.RobotExecutors;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyou.ucf.basedoc.service.itf.IEnterpriseBankAcctService;
import com.yonyou.ucf.mdd.ext.base.user.User;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.sql.SqlHelper;
import com.yonyou.ucf.mdd.ext.sys.service.UserService;
import com.yonyou.yonbip.ctm.accelerator.threadpool.CtmThreadPoolExecutor;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.bankreceipt.dto.TenantDTO;
import com.yonyoucloud.fi.cmp.checkStock.service.enums.CashType;
import com.yonyoucloud.fi.cmp.checkstock.CheckStatus;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.checkstock.CheckStockCharacterDef;
import com.yonyoucloud.fi.cmp.checkstockapply.CheckStockApply;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.enums.CheckDirection;
import com.yonyoucloud.fi.cmp.enums.YesOrNoEnum;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckDir;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.PageUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.biz.base.Objectlizer;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * 重空凭证工作台服务类
 */
@Service
@Transactional(rollbackFor = RuntimeException.class)
@Slf4j
public class CheckStockServiceImpl implements CheckStockService {

    @Autowired
    private CmCommonService cmCommonService;
    @Autowired
    private AutoConfigService autoConfigService;
    @Autowired
    private CheckStatusService checkStatusService;
    @Autowired
    private UserService userService;
    @Autowired
    IEnterpriseBankAcctService iEnterpriseBankAcctService;
    @Autowired
    EnterpriseBankQueryService enterpriseBankService;

    @Autowired
    YmsOidGenerator ymsOidGenerator;
    /**
     * 重空凭证工作台 兑付
     *
     * @param rows
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONArray checkStockGetCash(CtmJSONArray rows, CtmJSONObject obj) throws Exception {
        if (null == rows || rows.size() < 1) {
            //没有可操作的数据！
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100721"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800E1", "请选择单据！") /* "请选择单据！" */);
        }
        if (!Objects.isNull(obj.getString("cashDate")) && !Objects.isNull(obj.getString("drawerDate")) && DateUtils.convertToDate(obj.getString(
                "cashDate").substring(0, 10)+" 00:00:00", DateUtils.DATE_TIME_PATTERN).compareTo(DateUtils.convertToDate(obj.getString(
                        "drawerDate").substring(0, 10)+" 00:00:00", DateUtils.DATE_TIME_PATTERN)) < 0) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_217A7A3C04E00003", "兑付日期不能早于出票日期！") /* "兑付日期不能早于出票日期！" */);
        }
        List<Long> ids = getIdsByRows(rows);
        List<CheckStock> checkStorks = getCpmCheckStocks(ids);
        checkGetCash(checkStorks);
        setDef(obj, checkStorks);
        for (CheckStock checkStork : checkStorks) {
//            Date cashDate = new Date();
//            Date businessDate = BillInfoUtils.getBusinessDate();
//            if (businessDate != null) {
//                cashDate = businessDate;
//            }
            if (!Objects.isNull(obj.getString("cashDate"))) {
                checkStork.setCashDate(DateUtils.convertToDate(obj.getString("cashDate").substring(0, 10)+" 00:00:00", DateUtils.DATE_TIME_PATTERN));
            }
            if (!Objects.isNull(obj.getString("drawerDate"))) {
                checkStork.setDrawerDate(DateUtils.convertToDate(obj.getString("drawerDate").substring(0, 10)+" 00:00:00", DateUtils.DATE_TIME_PATTERN));
            }
            if (!Objects.isNull(obj.getBigDecimal("amount"))) {
                checkStork.setAmount(obj.getBigDecimal("amount"));
            }
            if (!Objects.isNull(obj.getString("drawerName"))) {
                checkStork.setDrawerName(obj.getString("drawerName"));
            }
            if (!Objects.isNull(obj.getString("payeeName"))) {
                checkStork.setPayeeName(obj.getString("payeeName"));
            }
            if (!Objects.isNull(obj.getString("checkpurpose"))) {
                checkStork.setCheckpurpose(obj.getShort("checkpurpose"));
            }
        }
        checkStatusService.recordCheckStatus(checkStorks, CmpCheckStatus.Cashed.getValue());
        List<CheckStock> updateList = updateStockListByCash(checkStorks, CmpCheckStatus.Cashed);
        MetaDaoHelper.update(CheckStock.ENTITY_NAME, updateList);
        cmCommonService.refreshPubTs(CheckStock.ENTITY_NAME, ids, rows);
        return rows;
    }

    /**
     * 重空凭证工作台 取消兑付
     *
     * @param rows
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONArray checkStockCancelCash(CtmJSONArray rows) throws Exception{
        if (null==rows||rows.size()<1){
            //没有可操作的数据！
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100721"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800E1","请选择单据！") /* "请选择单据！" */);
        }
        List<Long> ids=getIdsByRows(rows);
        List<CheckStock> checkStorks = getCpmCheckStocks(ids);
        checkCancelCash(checkStorks);
        checkStatusService.recordCheckStatusByCancelCash(checkStorks);
        List<CheckStock> updateList = updateStockListByCash(checkStorks, null);
        MetaDaoHelper.update(CheckStock.ENTITY_NAME,updateList);
        cmCommonService.refreshPubTs(CheckStock.ENTITY_NAME, ids, rows);
        return rows;
    }

    private void setDef(CtmJSONObject obj, List<CheckStock> checkStorks) {
        if (obj.get("characterDef") == null) {
            return;
        }
        BizObject bizObject = Objectlizer.convert(obj.getObject("characterDef", LinkedHashMap.class), CheckStockCharacterDef.ENTITY_NAME);
        if (bizObject == null) {
            return;
        }
        for (CheckStock item : checkStorks) {
            //特征
            //设置特征状态
            //原来没有值的时候，新增
            if (item.getCharacterDef() == null) {
                bizObject.setId(String.valueOf(ymsOidGenerator.nextId()));
                bizObject.setEntityStatus(EntityStatus.Insert);
            } else {
                bizObject.setEntityStatus(EntityStatus.Update);
            }
            item.put("characterDef", bizObject);

        }
    }

    @Override
    public CheckStock getCheckStockById(Long id) throws Exception {
        List<Map<String, Object>> checkStockList = MetaDaoHelper.queryById(CheckStock.ENTITY_NAME, "*", id);
        if (CollectionUtils.isNotEmpty(checkStockList)) {
            CheckStock checkStock = new CheckStock();
            checkStock.init(checkStockList.get(0));
            return checkStock;
        }
        return null;
    }

    /**
     * 重空凭证工作台 领用
     * @param rows
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONArray checkStockGetUsed(CtmJSONArray rows,String custNo) throws Exception{
        if (null==rows||rows.size()<1){
            //没有可操作的数据！
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100721"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800E1","请选择单据！") /* "请选择单据！" */);
        }
        List<Long> ids=getIdsByRows(rows);
        List<CheckStock> checkStorks = getCpmCheckStocks(ids);
        checkGetUsed(checkStorks);
        checkUseByDept(checkStorks);
        checkStatusService.recordCheckStatus(checkStorks, CmpCheckStatus.Use.getValue());
        List<CheckStock> updateList = updateStockList(checkStorks, CmpCheckStatus.Use,custNo);
        MetaDaoHelper.update(CheckStock.ENTITY_NAME,updateList);
        cmCommonService.refreshPubTs(CheckStock.ENTITY_NAME, ids, rows);
        return rows;
    }

    /**
     * 重空凭证工作台 取消领用
     * @param rows
     * @return
     * @throws Exception
     */
    @Override
    public CtmJSONArray checkStockCancelUsed(CtmJSONArray rows) throws Exception{
        if (null==rows||rows.size()<1){
            //没有可操作的数据！
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100721"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041800E1","请选择单据！") /* "请选择单据！" */);
        }
        List<Long> ids=getIdsByRows(rows);
        List<CheckStock> checkStorks = getCpmCheckStocks(ids);
        checkCancelUsed(checkStorks);
        checkStatusService.recordCheckStatus(checkStorks, CmpCheckStatus.InStock.getValue());
        List<CheckStock> updateList = updateStockList(checkStorks, CmpCheckStatus.InStock,null);
        MetaDaoHelper.update(CheckStock.ENTITY_NAME,updateList);
        cmCommonService.refreshPubTs(CheckStockApply.ENTITY_NAME, ids, rows);
        return rows;
    }

    /**
     * 重空凭证工作台，领用/取消领用，批量修改支票状态
     * @param checkStocks
     * @return
     */
    private List<CheckStock> updateStockList(List<CheckStock> checkStocks,CmpCheckStatus checkBillStatus,String custNo) {
        if (CollectionUtils.isEmpty(checkStocks)){
            return null;
        }
        List<CheckStock> updateList = new ArrayList<>();
        for (CheckStock vo : checkStocks){
            CheckStock update = new CheckStock();
            update.setEntityStatus(EntityStatus.Update);
            update.setId(vo.getId());
            update.setCheckBillStatus(checkBillStatus.getValue());
            update.setCustNo(custNo);
            //设置是否可使用字段
            if(CmpCheckStatus.InStock.getValue().equals(checkBillStatus.getValue())){ //取消领用
                update.setIsUsed((short)0);
                // 领用人
                update.setRecipient(null);
                update.setRecipientname(null);
                // 领用日期
                update.setReceivedate(null);
            } else {
                //领用
                update.setIsUsed((short)1);
                // 领用人
                update.setRecipient(AppContext.getCurrentUser().getYhtUserId()+"");
                update.setRecipientname(AppContext.getCurrentUser().getName()+"");
                // 领用日期
                update.setReceivedate(new Date());
            }
            updateList.add(update);
        }
        return updateList;
    }

    /**
     * 重空凭证工作台，兑付/取消兑付，批量修改支票状态
     * @param checkStocks
     * @return
     */
    private List<CheckStock> updateStockListByCash(List<CheckStock> checkStocks,CmpCheckStatus checkBillStatus) {
        if (CollectionUtils.isEmpty(checkStocks)) {
            return null;
        }
        List<CheckStock> updateList = new ArrayList<>();
        for (CheckStock vo : checkStocks) {
            CheckStock update = new CheckStock();
            update.setEntityStatus(EntityStatus.Update);
            update.setId(vo.getId());
            //设置兑付前的转态
            //设置是否可使用字段
            if (checkBillStatus != null && CmpCheckStatus.Cashed.getValue().equals(checkBillStatus.getValue())) { //已兑付
                update.setIsUsed((short) 1);
                update.setCashType(CashType.Manual.getIndex());
                update.setBeforeCashStatus(vo.getCheckBillStatus());
                update.setCashDate(vo.getCashDate());
                update.setCashPerson(AppContext.getCurrentUser().getName());
                update.setCheckBillStatus(checkBillStatus.getValue());
                if (!Objects.isNull(vo.getDate("cashDate"))) {
                    update.setCashDate(vo.getDate("cashDate"));
                }
                if (!Objects.isNull(vo.getDate("drawerDate"))) {
                    update.setDrawerDate(vo.getDate("drawerDate"));
                }
                if (!Objects.isNull(vo.getBigDecimal("amount"))) {
                    update.setAmount(vo.getBigDecimal("amount"));
                }
                if (!Objects.isNull(vo.getString("drawerName"))) {
                    update.setDrawerName(vo.getString("drawerName"));
                }
                if (!Objects.isNull(vo.getString("payeeName"))) {
                    update.setPayeeName(vo.getString("payeeName"));
                }
                if (!Objects.isNull(vo.get("characterDef"))) {
                    update.set("characterDef",vo.get("characterDef"));
                }
                if (!Objects.isNull(vo.getString("checkpurpose"))) {
                    update.setCheckpurpose(vo.getShort("checkpurpose"));
                }
            } else {
                //取消兑付  清空兑付时更新的字段；如付票清空金额、出票日期、出票人员、收款人名称、兑付日期、重空凭证用途、兑付方式字段；如收票清空兑付日期、兑付方式字段
                update.setIsUsed((short) 0);
                update.setCheckBillStatus(vo.getBeforeCashStatus());
                update.setCashType(null);
                update.setCashPerson(null);
                update.setCashDate(null);
                update.setBeforeCashStatus(null);
                update.setCheckpurpose(null);
                update.setPayeeName(null);
                if (CmpCheckDir.Cash.getValue().equals(vo.getCheckBillDir())) {
                    update.setDrawerDate(null);
                    update.setAmount(null);
                    update.setDrawerName(null);
                }
            }
            updateList.add(update);
        }
        return updateList;
    }


    private void checkCancelUsed(List<CheckStock> checkStorks) {
        checkStorks.forEach(stock ->{
            if (!CmpCheckStatus.Use.getValue().equals(stock.getCheckBillStatus())){
                //存在未领取的数据，不能取消领取
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101340"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CAC294404F00000","存在未领用的数据，不能取消领用") /* "存在未领用的数据，不能取消领用！" */);
            }
            if (stock.getOccupy() != null && stock.getOccupy() == YesOrNoEnum.YES.getValue()) {
                // 若支票编号已被预占，则提示保存失败
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2290742C04B00007", "支票编号已被预占，不能取消领用！"));
            }
        });
    }

    /**
     * 领用校验
     * @param checkStorks
     */
    private void checkGetUsed(List<CheckStock> checkStorks) {
        checkStorks.forEach(stock ->{
            if(!CmpCheckDir.Cash.getValue().equals(stock.getCheckBillDir())){
                //存在收票的数据，不能被领取
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101341"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A0508002A", "存在支票方向为收票的数据，不能被领用！") /* "存在支票方向为收票的数据，不能被领用！" */);
            }
            if(!CmpCheckStatus.Use.getValue().equals(stock.getCheckBillStatus()) && !CmpCheckStatus.InStock.getValue().equals(stock.getCheckBillStatus())){
                //存在未入库的数据，不能被领取
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101342"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CA8C2E404F00001","存在未入库的数据，不能被领用！") /* "存在未入库的数据，不能被领用！" */);

            }
        });
    }

    /**
     * 后台校验用户所属的部门领用后的支票张数是否超过所属会计主体限制，若超过，错误性提示“领用失败！超过按部门领用张数限制”。
     * 先找到领用用户的部门，然后查询status表的所有该部门下的数据，再加一个条件会计主体
     * 先找到最后一次领用 且后面没有取消领用过，这个认为是领用成功的
     * 根据会计主体分类 每个会计主体下的领用张数
     * 根据现金基础参数配置，按部门领用是否开启，领用张数限制（张）是否有值，是否超过领用张数限制
     *
     * @param checkStorks
     */
    private void checkUseByDept(List<CheckStock> checkStorks) throws Exception {
        Map<String, List<CheckStock>> checkStockGroupByAcc = checkStorks.stream().collect(Collectors.groupingBy(CheckStock::getAccentity));
        String department = null;
        List<String> deptListByStaffId = null;
        try {
            User user = userService.find(Long.valueOf(InvocationInfoProxy.getIdentityId()));
            department = user.getDepartment();
            Object hstaff = user.get("hstaff");
            if (hstaff != null) {
                deptListByStaffId = checkStatusService.getDeptListByStaffId(String.valueOf(hstaff));
            }
        } catch (Exception e) {
            log.error("查询用户部门失败", e);
        }
        if (CollectionUtils.isNotEmpty(deptListByStaffId)) {
            for (String dept : deptListByStaffId) {
                checkUseByStaffDept(checkStockGroupByAcc, dept);
            }
        } else if (StringUtils.isNotBlank(department)) {
            checkUseByStaffDept(checkStockGroupByAcc, department);
        } else {
            log.error("当前用户没有部门,用户身份id={}", InvocationInfoProxy.getIdentityId());
        }
    }


    public void checkUseByStaffDept(Map<String, List<CheckStock>> checkStockGroupByAcc, String department) throws Exception {
        List<CheckStatus> checkStatuses = checkStatusService.getCheckStatusByAccAndOpDept(checkStockGroupByAcc.keySet(), department);
        Map<String, List<CheckStatus>> statusGroupByAcc = checkStatuses.stream().collect(Collectors.groupingBy(CheckStatus::getAccentity));
        Map<String, List<CheckStatus>> usedCheckStockByAcc = new HashMap<>();//每个会计主体下的所有已经领用过的支票
        Map<String, AutoConfig> autoConfigByAcc = new HashMap<>();//每个会计主体下的领用张数限制（张）
        for (String acc : statusGroupByAcc.keySet()) {
            AutoConfig autoConfig = autoConfigService.getAutoConfigByAcc(acc);
            if (autoConfig != null && autoConfig.getCheckUseByDept() != null && autoConfig.getCheckUseByDept()) {
                autoConfigByAcc.put(acc, autoConfig);
                //一个会计主体下的所有支票所有的状态
                List<CheckStatus> checkStatusList = statusGroupByAcc.get(acc);
                List<CheckStatus> usedCheckStock = new ArrayList<>();
                Map<Long, List<CheckStatus>> statusGroupByCheckId = checkStatusList.stream().collect(Collectors.groupingBy(CheckStatus::getCheckId));
                //判断这张支票是否是领用过的
                for (Long checkId : statusGroupByCheckId.keySet()) {
                    List<CheckStatus> statusListForSameCheckId = statusGroupByCheckId.get(checkId);
                    statusListForSameCheckId.sort(Comparator.comparing(CheckStatus::getPubts).reversed());
                    //先找到最后一次领用 且后面没有取消领用过，这个认为是领用成功的
                    for (CheckStatus checkStatus : statusListForSameCheckId) {
                        if (checkStatusService.isCancelUsed(checkStatus)) {
                            break;
                        }
                        if (CmpCheckStatus.Use.getValue().equals(checkStatus.getAfterCheckBillStatus())) {
                            usedCheckStock.add(checkStatus);
                            break;
                        }
                    }
                }
                usedCheckStockByAcc.put(acc, usedCheckStock);
            }
        }
        //校验是否超过领用张数  “领用失败！超过按部门领用张数限制”
        for (String acc : autoConfigByAcc.keySet()) {
            AutoConfig autoConfig = autoConfigByAcc.get(acc);
            if (autoConfig.getUseNumLimit() != null) {
                //计算领用过的加上本次领用的张数
                int useCount = 0;
                List<CheckStock> checkStocks = checkStockGroupByAcc.get(acc);
                List<CheckStatus> checkStatus = usedCheckStockByAcc.get(acc);
                useCount += checkStocks.size();
                if (CollectionUtils.isNotEmpty(checkStatus)) {
                    useCount += checkStatus.size();
                }
                if (useCount > autoConfig.getUseNumLimit()) {
                    log.error("会计主体：{}，领用失败！超过按部门领用张数限制", acc);
                    //领用失败！超过按部门领用张数[%s]张限制
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101343"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4A17A205900006", "领用失败！超过按部门领用张数[%s]张限制") /* "领用失败！超过按部门领用张数[%s]张限制" */, autoConfig.getUseNumLimit()));
                }
            }
            BigDecimal totalAccount = new BigDecimal("0");//已领用但目前支票状态不等于已兑付的支票的金额总和
            /**
             * 后台校验用户所属的部门已领用 但目前支票状态不等于已兑付、处置中、已处置（已作废，已退回，已挂失）的支票
             * 当前系统日期距离领用日期的天数最长天数是否超过所属会计主体最长未兑付期限制，
             * 若超过，错误性提示“领用失败！超过按部门领用最长未兑付期限制”
             */
            if (autoConfig.getMaxNotCashDateLimit() != null) {
                //已领用 但目前支票状态不等于已兑付的支票
                List<CheckStatus> checkStatus = usedCheckStockByAcc.get(acc);
                for (CheckStatus status : checkStatus) {
                    CheckStock checkStockById = getCheckStockById(status.getCheckId());
                    if (checkStockById == null || StringUtils.isEmpty(checkStockById.getCheckBillStatus())) {
                        continue;
                    }
                    if (StringUtils.equalsAny(checkStockById.getCheckBillStatus(), CmpCheckStatus.Cashed.getValue(), CmpCheckStatus.Disposal.getValue(), CmpCheckStatus.Cancle.getValue(), CmpCheckStatus.Cancel.getValue(), CmpCheckStatus.Loss.getValue())) {
                        continue;
                    }
                    if (checkStockById.getAmount() != null) {
                        totalAccount = totalAccount.add(checkStockById.getAmount());
                    }
                    if (status.getOperatorDate() != null) {
                        if (DateUtils.dateBetween(status.getOperatorDate(), new Date()) > autoConfig.getMaxNotCashDateLimit()) {
                            log.error("status_id={},领用失败！超过按部门领用最长未兑付期限制", (Long) status.getId());
                            //领用失败！超过按部门领用最长未兑付期[%s]天限制
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101344"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4A17CC05800008", "领用失败！超过按部门领用最长未兑付期[%s]天限制") /* "领用失败！超过按部门领用最长未兑付期[%s]天限制" */, autoConfig.getMaxNotCashDateLimit()));
                        }
                    }
                }
            }
            /**
             * 后台校验用户所属的部门已领用但目前支票状态不等于已兑付、处置中、已处置的支票
             * 支票金额总和是否超过所属会计主体未兑付支票总金额限制，
             * 若超过，错误性提示“领用失败！超过按部门领用未兑付支票总金额限制”。
             */
            if (autoConfig.getNotCashCheckTotalAmountLimit() != null) {
                //比较大小，返回int类型。0（相等） 1（大于） -1（小于）
                if (totalAccount.compareTo(autoConfig.getNotCashCheckTotalAmountLimit()) > 0) {
                    log.error("会计主体：{}，领用失败！超过按部门领用未兑付支票总金额限制", acc);
                    //领用失败！超过按部门领用未兑付支票总金额[%s]元限制
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101345"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4A13E005900003", "领用失败！超过按部门领用未兑付支票总金额[%s]元限制") /* "领用失败！超过按部门领用未兑付支票总金额[%s]元限制" */, autoConfig.getNotCashCheckTotalAmountLimit()));
                }
            }
        }
    }



    /**
     * 兑付校验
     *
     * @param checkStorks
     */
    private void checkGetCash(List<CheckStock> checkStorks) {
        checkStorks.forEach(stock -> {
            boolean canCash = false;
            //③支票方向=收票，支票状态=已入库的支票可以直接兑付
            if (CheckDirection.Receive.getIndex().equals(stock.getCheckBillDir()) && CmpCheckStatus.InStock.getValue().equals(stock.getCheckBillStatus())) {
                canCash = true;
            }
            //支票方向=付票、重空凭证领用参数开启、支票状态=已领用；
            //支票方向=付票、重空凭证领用参数关闭、支票状态=已入库
            if (CheckDirection.Pay.getIndex().equals(stock.getCheckBillDir())) {
                try {
                    if (autoConfigService.getCheckStockCanUse()) {
                        canCash = CmpCheckStatus.Use.getValue().equals(stock.getCheckBillStatus());
                    } else {
                        canCash = CmpCheckStatus.InStock.getValue().equals(stock.getCheckBillStatus());
                    }
                } catch (Exception e) {
                    log.error("checkGetCash Exception:", e);
                }
            }
            if (!canCash) {
                //兑付失败！支票【支票编号】的状态不可直接兑付。
                //兑付失败！支票[%s]的状态不可直接兑付。
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101346"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4A148005900009", "兑付失败！支票[%s]的状态不可直接兑付。") /* "兑付失败！支票[%s]的状态不可直接兑付。" */, stock.getCheckBillNo()));
            }
            if (stock.getOccupy() != null && stock.getOccupy() == YesOrNoEnum.YES.getValue()) {
                throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_22A82BC205500006", "重空凭证编号：[%s]已经被单据引用，不可手工兑付！") /* "重空凭证编号：[%s]已经被单据引用，不可手工兑付！" */, stock.getCheckBillNo()));
            }
        });
    }

    /**
     * 兑付方式=手工直接兑付的支票可以取消兑付
     *
     * @param checkStorks
     */
    private void checkCancelCash(List<CheckStock> checkStorks) {
        checkStorks.forEach(stock -> {
            if (stock.getCashType() == null || CashType.Manual.getIndex() != stock.getCashType()) {
                //取消兑付失败！支票【支票编号】的兑付方式不是手工直接兑付，不可取消兑付。
                //取消兑付失败！支票[%s]的兑付方式不是手工直接兑付，不可取消兑付。
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101347"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1C4A152A05800008", "取消兑付失败！支票[%s]的兑付方式不是手工直接兑付，不可取消兑付。") /* "取消兑付失败！支票[%s]的兑付方式不是手工直接兑付，不可取消兑付。" */, stock.getCheckBillNo()));
            }
        });
    }

    /**
     * 获取主键id
     * @param rows
     * @return
     */
    private List<Long> getIdsByRows(CtmJSONArray rows) {
        if (null==rows||rows.size()<1){
            return null;
        }
        List<Long> ids=new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            CtmJSONObject checkAll = rows.getJSONObject(i);
            Long id = checkAll.getLong("id");
            ids.add(id);
        }
        return ids;
    }

    /**
     * 根据主表id获取“重空凭证工作台”单据
     * @param ids
     * @return
     * @throws Exception
     */
    private List<CheckStock> getCpmCheckStocks(List<Long> ids) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.appendCondition(QueryCondition.name("id").in(ids));
        querySchema.addCondition(queryConditionGroup);
        return MetaDaoHelper.queryObject(CheckStock.ENTITY_NAME, querySchema, null);
    }


    @Override
    public void migrationStockToStatus() throws Exception {
        String QUERY_ALL_TENANT = "com.yonyoucloud.fi.cmp.mapper.TenantMapper.queryAllTenant";
        log.error("开始执行历史支票库存数据升级");
        // 查询所有的租户信息
        List<TenantDTO> tenantDTOList = SqlHelper.selectList(QUERY_ALL_TENANT);
        // 通过机器人异步执行
        CtmThreadPoolExecutor ctmThreadPoolExecutor = AppContext.getBean(CtmThreadPoolExecutor.class);
        for (TenantDTO dto : tenantDTOList) {
            try {
                RobotExecutors.runAs(dto.getYtenantId(), new Callable() {
                    @Override
                    public Object call() throws Exception {
                        String beforeCheckBillStatus = "66";//标记为迁移的数据
                        String inStockBeforeStatus = "67";//标记为迁移时补的入库数据
                        QuerySchema querySchemaStatus = QuerySchema.create().addSelect(QuerySchema.PARTITION_ALL);
                        querySchemaStatus.appendQueryCondition(QueryCondition.name("beforeCheckBillStatus").eq(beforeCheckBillStatus));
                        List<CheckStatus> checkStatusList = MetaDaoHelper.queryObject(CheckStatus.ENTITY_NAME, querySchemaStatus, null);
                        if (CollectionUtils.isNotEmpty(checkStatusList)) {
                            log.error("租户：{}下数据已经迁移过了", dto.getYtenantId());
                        } else {
                            QuerySchema querySchema = QuerySchema.create().addSelect(QuerySchema.PARTITION_ALL);
                            QueryConditionGroup conditionGroup = new QueryConditionGroup();
                            querySchema.appendQueryCondition(conditionGroup);
                            List<CheckStock> checkStocks  = PageUtils.logicPageQuery(querySchema,conditionGroup,CheckStock.ENTITY_NAME);
                            if (CollectionUtils.isEmpty(checkStocks)) {
                                log.error("查询支票库存历史数据为空");
                            } else {
                                batchMigrationStocks(checkStocks, beforeCheckBillStatus);
                            }
                        }
                        QuerySchema querySchemaInStockStatus = QuerySchema.create().addSelect(QuerySchema.PARTITION_ALL);
                        querySchemaInStockStatus.appendQueryCondition(QueryCondition.name("beforeCheckBillStatus").eq(inStockBeforeStatus));
                        List<CheckStatus> inStockCheckStatusList = MetaDaoHelper.queryObject(CheckStatus.ENTITY_NAME, querySchemaInStockStatus, null);
                        if (CollectionUtils.isNotEmpty(inStockCheckStatusList)) {
                            log.error("租户：{}下数据已经迁移过了", dto.getYtenantId());
                        } else {
                            QuerySchema querySchema = QuerySchema.create().addSelect(QuerySchema.PARTITION_ALL);
                            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
                            conditionGroup.appendCondition(QueryCondition.name("checkBillStatus").not_eq(1));
                            querySchema.appendQueryCondition(conditionGroup);
                            List<CheckStock> checkStocks  = PageUtils.logicPageQuery(querySchema,conditionGroup,CheckStock.ENTITY_NAME);
                            if (CollectionUtils.isEmpty(checkStocks)) {
                                log.error("查询支票库存历史数据为空");
                            } else {
                                batchMigrationAfterInStock(checkStocks, inStockBeforeStatus);
                            }
                        }
                        return null;
                    }
                }, ctmThreadPoolExecutor.getThreadPoolExecutor());
            } catch (Exception e) {
                log.error("migrationStockToStatus error", e);
            }
        }
    }

    /**
     * 领用时校验账户权限
     * @param accentity
     * @param checkBookNo
     * @throws Exception
     */
    @Override
    public void checkAccAuthority(String accentity,String custNo, String checkBookNo) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("drawerAcct");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("checkBookNo").eq(checkBookNo),QueryCondition.name("accentity").eq(accentity));
        querySchema.addCondition(group);
        List<CheckStock> stock = MetaDaoHelper.queryObject(CheckStock.ENTITY_NAME, querySchema, null);
        // 银行账户ID
        String drawerAcct = stock.get(0).getDrawerAcct();
        EnterpriseBankAcctVOWithRange enterpriseBankAcctVOWithRange = enterpriseBankService.queryEnterpriseBankAcctVOWithRangeById(drawerAcct);
        log.error("适用范围接口返回信息" + enterpriseBankAcctVOWithRange);
        if (Objects.nonNull(enterpriseBankAcctVOWithRange)) {
            List<OrgRangeVO> accountApplyRange = enterpriseBankAcctVOWithRange.getAccountApplyRange();
            List<String> orgIdList= accountApplyRange.stream().map(OrgRangeVO::getRangeOrgId).collect(Collectors.toList());
            log.error("orgIdList返回信息" + orgIdList);
            if (CollectionUtils.isNotEmpty(orgIdList) && !orgIdList.contains(custNo)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101348"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D5E84CC04980000","领用组织无该支票银行账户权限，不可领用! ") /* "领用组织无该支票银行账户权限，不可领用! " */);
            }
        }
    }

    public void batchMigrationStocks(List<CheckStock> checkStocks, String beforeCheckBillStatus) throws Exception {
        List<List<CheckStock>> partition = Lists.partition(checkStocks, 100);
        for (List<CheckStock> checkStockList : partition) {
            checkStatusService.recordCheckStatusForMigration(checkStockList, beforeCheckBillStatus);
        }
        log.error("************【{}】条数据，迁移支票库存数据到支票状态表************", checkStocks.size());
    }

    public void batchMigrationAfterInStock(List<CheckStock> checkStocks, String inStockBeforeStatus) throws Exception {
        List<List<CheckStock>> partition = Lists.partition(checkStocks, 100);
        for (List<CheckStock> checkStockList : partition) {
            checkStatusService.recordInStockCheckStatusForMigration(checkStockList, inStockBeforeStatus);
        }
        log.error("************【{}】条数据，补录已入库支票库存数据到支票状态表************", checkStocks.size());
    }
}
