package com.yonyoucloud.fi.cmp.checkStock.service;

import com.google.common.base.Joiner;
import com.yonyou.diwork.ott.exexutors.RobotExecutors;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.base.user.User;
import com.yonyou.ucf.mdd.ext.bill.rule.util.BillInfoUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.sys.service.UserService;
import com.yonyoucloud.fi.cmp.checkinventory.CheckInventory;
import com.yonyoucloud.fi.cmp.checkinventory.CheckInventory_b;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManage;
import com.yonyoucloud.fi.cmp.checkmanage.CheckManageDetail;
import com.yonyoucloud.fi.cmp.checkstock.CheckStatus;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.vo.checkstock.CheckDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QueryOrderby;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class CheckStatusServiceImpl implements CheckStatusService {
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    UserService userService;

    @Override
    public void recordCheckStatus(List<CheckStock> checkStocks, String afterCheckBillStatus) throws Exception {
        List<CheckStatus> checkStatusList = new ArrayList<>();
        for (CheckStock checkStock : checkStocks) {
            checkStatusList.add(buildCheckStatus(checkStock, afterCheckBillStatus, null, null));
        }
        CmpMetaDaoHelper.insert(CheckStatus.ENTITY_NAME, checkStatusList);
    }

    public void recordCheckStatus(List<CheckStock> checkStocks, String afterCheckBillStatus, Long maiid) throws Exception {
        List<CheckStatus> checkStatusList = new ArrayList<>();
        for (CheckStock checkStock : checkStocks) {
            checkStatusList.add(buildCheckStatus(checkStock, afterCheckBillStatus, maiid, null));
        }
        CmpMetaDaoHelper.insert(CheckStatus.ENTITY_NAME, checkStatusList);
    }


    @Override
    public CheckStatus buildCheckStatus(CheckStock checkStock, String afterCheckBillStatus, Long maiid, CheckManage checkManage) {
        String department = null;
        String operator = InvocationInfoProxy.getIdentityId(); //身份id
        String staffId = InvocationInfoProxy.getUserid();
        try {
            User user = userService.find(Long.valueOf(InvocationInfoProxy.getIdentityId()));
            department = user.getDepartment();
            Object hstaff = user.get("hstaff");
            if (hstaff != null) {
                staffId = String.valueOf(hstaff);
            }
        } catch (Exception e) {
            log.error("查询用户部门失败", e);
        }
        CheckStatus checkStatus = new CheckStatus();
        checkStatus.setAccentity(checkStock.getAccentity());//会计主体
        checkStatus.setCheckBillDir(checkStock.getCheckBillDir());//支票方向
        checkStatus.setCheckBillNo(checkStock.getCheckBillNo());//支票编号
        checkStatus.setCheckBillType(checkStock.getCheckBillType());//支票类型
        checkStatus.setCheckId(Long.parseLong(checkStock.getId().toString()));
        checkStatus.setBeforeCheckBillStatus(checkStock.getCheckBillStatus());//更新前支票状态
        checkStatus.setAfterCheckBillStatus(afterCheckBillStatus);//更新后支票状态
        Date operatorDate = new Date();
        if (checkManage == null) {
            checkManage = getCheckManageByCheckId(Long.parseLong(checkStock.getId().toString()));
        }
        if (CmpCheckStatus.InStock.getValue().equals(afterCheckBillStatus)) {
            if (checkStock.getBusiDate() != null) {
                operatorDate = checkStock.getBusiDate();
            }
            //盘点完以后恢复为已入库时，取重空凭证盘点的“审批通过日期”
            if (CmpCheckStatus.Inventory.getValue().equals(checkStock.getCheckBillStatus())) {
                operatorDate = new Date();
            }
        }
        //支票状态=处置中,已作废、已挂失、已退回的，取重空凭证处置的“单据日期”
        if (StringUtils.equalsAny(afterCheckBillStatus, CmpCheckStatus.Disposal.getValue(), CmpCheckStatus.Cancle.getValue(), CmpCheckStatus.Loss.getValue(), CmpCheckStatus.Cancel.getValue())) {
            if (checkManage != null) {
                if (checkManage.getBilldate() != null) {
                    operatorDate = checkManage.getBilldate();
                }
                //支票状态=处置中，取重空凭证处置的“创建人”
                if (CmpCheckStatus.Disposal.getValue().equals(afterCheckBillStatus) && checkManage.getCreatorId() != null) {
                    operator = String.valueOf(checkManage.getCreatorId());
                }
            }
        }
        if (CmpCheckStatus.Cashed.getValue().equals(afterCheckBillStatus) && checkStock.getCashDate() != null) {
            operatorDate = checkStock.getCashDate();
        }
        //支票状态=已作废、已挂失、已退回，取重空凭证处置的“处置人”
        if (StringUtils.equalsAny(afterCheckBillStatus, CmpCheckStatus.Cancle.getValue(), CmpCheckStatus.Loss.getValue(), CmpCheckStatus.Cancel.getValue())) {
            if (checkManage != null) {
                if (checkManage.getAuditorId() != null) {
                    operator = String.valueOf(checkManage.getAuditorId());
                }
            }
        }
        //支票状态=盘点中，取重空凭证盘点的“盘点人”
        //支票状态=盘点中，取重空凭证盘点的“单据创建日期“
        if (CmpCheckStatus.Inventory.getValue().equals(afterCheckBillStatus) && maiid != null) {
            CheckInventory checkInventory = getInventoryByMaiid(maiid);
            if (checkInventory != null) {
                if (checkInventory.getCreatorId() != null) {
                    operator = String.valueOf(checkInventory.getCreatorId());
                }
                if (checkInventory.getCreateDate() != null) {
                    operatorDate = checkInventory.getCreateDate();
                }
            }
        }
        //支票状态=已领用，记录领用支票的业务日期
        if (CmpCheckStatus.Use.getValue().equals(afterCheckBillStatus) && BillInfoUtils.getBusinessDate() != null) {
            operatorDate = BillInfoUtils.getBusinessDate();
        }
        //支票状态=已开票，取重空凭证工作台的“出票日期”
        if (CmpCheckStatus.BillOver.getValue().equals(afterCheckBillStatus) && checkStock.getDrawerDate() != null) {
            operatorDate = checkStock.getDrawerDate();
        }
        checkStatus.setOperator(operator);//操作人
        List<String> deptListByStaffId = getDeptListByStaffId(staffId);
        if (CollectionUtils.isNotEmpty(deptListByStaffId)) {
            department = Joiner.on(",").join(deptListByStaffId);
        }
        checkStatus.setOperatorDept(department);//操作人部门

        checkStatus.setOperatorDate(operatorDate);//操作日期
        checkStatus.setId(ymsOidGenerator.nextId());
        checkStatus.setEntityStatus(EntityStatus.Insert);
        return checkStatus;
    }

    public CheckStatus buildInStockCheckStatusForMigration(CheckStock checkStock, String beforeCheckBillStatus) {
        CheckStatus checkStatus = new CheckStatus();
        checkStatus.setAccentity(checkStock.getAccentity());//会计主体
        checkStatus.setCheckBillDir(checkStock.getCheckBillDir());//支票方向
        checkStatus.setCheckBillNo(checkStock.getCheckBillNo());//支票编号
        checkStatus.setCheckBillType(checkStock.getCheckBillType());//支票类型
        checkStatus.setCheckId(checkStock.getId());
        checkStatus.setBeforeCheckBillStatus(beforeCheckBillStatus);//更新前支票状态
        checkStatus.setAfterCheckBillStatus(CmpCheckStatus.InStock.getValue());//更新后支票状态
        String operator = "";
        Date operatorDate = checkStock.getBusiDate();
        operator = String.valueOf(checkStock.getCreatorId());
        checkStatus.setOperator(operator);//操作人
        log.error("checkstockid:{},ytenantid:{}", checkStock.getId(), checkStock.getYtenantId());
        String department = "";
        checkStatus.setOperatorDept(department);//操作人部门
        checkStatus.setOperatorDate(operatorDate);//操作日期
        checkStatus.setPubts(operatorDate);
        checkStatus.setId(ymsOidGenerator.nextId());
        checkStatus.setYtenantId(checkStock.getYtenantId());
        return checkStatus;
    }

    public CheckStatus buildCheckStatusForMigration(CheckStock checkStock, String beforeCheckBillStatus) {
        CheckStatus checkStatus = new CheckStatus();
        checkStatus.setAccentity(checkStock.getAccentity());//会计主体
        checkStatus.setCheckBillDir(checkStock.getCheckBillDir());//支票方向
        checkStatus.setCheckBillNo(checkStock.getCheckBillNo());//支票编号
        checkStatus.setCheckBillType(checkStock.getCheckBillType());//支票类型
        checkStatus.setCheckId(checkStock.getId());
        checkStatus.setBeforeCheckBillStatus(beforeCheckBillStatus);//更新前支票状态
        checkStatus.setAfterCheckBillStatus(checkStock.getCheckBillStatus());//更新后支票状态
        String operator = "";
        Date operatorDate = checkStock.getCreateDate();
        CheckManage checkManage = getCheckManageByCheckId(checkStock.getId());
        if (CmpCheckStatus.InStock.getValue().equals(checkStock.getCheckBillStatus())) {
            operator = String.valueOf(checkStock.getCreatorId());
            operatorDate = checkStock.getBusiDate();
        }
        //支票状态=处置中的，取处置创建人,取重空凭证处置的“单据日期”
        if (CmpCheckStatus.Disposal.getValue().equals(checkStock.getCheckBillStatus())) {
            if (checkManage != null) {
                if (checkManage.getBilldate() != null) {
                    operatorDate = checkManage.getBilldate();
                }
                if (checkManage.getAuditorId() != null) {
                    operator = String.valueOf(checkManage.getCreatorId());
                }
            }
        }
        //支票状态=已作废、已挂失、已退回，取重空凭证处置的“处置人”
        //支票状态=已作废、已挂失、已退回，取重空凭证处置的“审批完成时间”；
        if (StringUtils.equalsAny(checkStock.getCheckBillStatus(), CmpCheckStatus.Cancle.getValue(), CmpCheckStatus.Loss.getValue(), CmpCheckStatus.Cancel.getValue())) {
            if (checkManage != null) {
                if (checkManage.getAuditorId() != null) {
                    operator = String.valueOf(checkManage.getAuditorId());
                }
                if (checkManage.getAuditDate() != null) {
                    operatorDate = checkManage.getAuditDate();
                }
            }
        }
        //支票状态=盘点中，取重空凭证盘点的“盘点人”
        //支票状态=盘点中，取重空凭证盘点的“单据创建日期“
        if (CmpCheckStatus.Inventory.getValue().equals(checkStock.getCheckBillStatus())) {
            CheckInventory checkInventory = getInventoryByCheckId(checkStock.getCheckBillNo(), checkStock.getId());
            if (checkInventory != null) {
                if (checkInventory.getCreatorId() != null) {
                    operator = String.valueOf(checkInventory.getCreatorId());
                }
                if (checkInventory.getCreateDate() != null) {
                    operatorDate = checkInventory.getCreateDate();
                }
            }
        }
        //操作日期 支票状态=已领用、在开票、兑付中
        if (StringUtils.equalsAny(checkStock.getCheckBillStatus(), CmpCheckStatus.Use.getValue(), CmpCheckStatus.Billing.getValue(), CmpCheckStatus.Cashing.getValue())) {
            operatorDate = null;
        }
        //支票状态=已开票，取重空凭证工作台的“出票日期”
        if (CmpCheckStatus.BillOver.getValue().equals(checkStock.getCheckBillStatus())) {
            operatorDate = checkStock.getDrawerDate();
        }
        //支票状态=已兑付，取重空凭证工作台的“兑付日期”；
        if (CmpCheckStatus.Cashed.getValue().equals(checkStock.getCheckBillStatus())) {
            operatorDate = checkStock.getCashDate();
        }
        checkStatus.setOperator(operator);//操作人
        log.error("checkstockid:{},ytenantid:{}", checkStock.getId(), checkStock.getYtenantId());
        List<String> deptListByStaffId = getDeptListByIdentityIdForMigration(checkStock.getYtenantId(), operator);
        String department = "";
        if (CollectionUtils.isNotEmpty(deptListByStaffId)) {
            department = Joiner.on(",").join(deptListByStaffId);
        }
        checkStatus.setOperatorDept(department);//操作人部门
        checkStatus.setOperatorDate(operatorDate);//操作日期
        checkStatus.setId(ymsOidGenerator.nextId());
        checkStatus.setYtenantId(checkStock.getYtenantId());
        return checkStatus;
    }


    /**
     * 根据支票库存id获取处置信息
     *
     * @param checkId
     * @return
     * @throws Exception
     */
    public CheckManage getCheckManageByCheckId(Long checkId) {
        try {
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = new QueryConditionGroup();
            group.addCondition(QueryCondition.name("checkid").eq(checkId));
            querySchema.addCondition(group);
            List<CheckManageDetail> checkManageDetails = MetaDaoHelper.queryObject(CheckManageDetail.ENTITY_NAME, querySchema, null);
            if (CollectionUtils.isNotEmpty(checkManageDetails)) {
                CheckManageDetail checkManageDetail = checkManageDetails.get(0);
                CheckManage checkManage = MetaDaoHelper.findById(CheckManage.ENTITY_NAME, checkManageDetail.getMainid());
                return checkManage;
            }
        } catch (Exception e) {
            log.error("getDisposerId exception", e);
        }
        return null;
    }


    /**
     * 根据支票库存id获取盘点人id
     *
     * @param maiid
     * @return
     * @throws Exception
     */
    public CheckInventory getInventoryByMaiid(Long maiid) {
        try {
            return MetaDaoHelper.findById(CheckInventory.ENTITY_NAME, maiid);
        } catch (Exception e) {
            log.error("getInventoryByCheckId exception", e);
        }
        return null;
    }

    public CheckInventory getInventoryByCheckId(String checkBillNo, Long checkId) {
        try {
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = new QueryConditionGroup();
            if (StringUtils.isNotEmpty(checkBillNo)) {
                group.addCondition(QueryCondition.name("checkBillNo").eq(checkId));
            }
            group.addCondition(QueryCondition.name("checkid").like(checkId));
            querySchema.addCondition(group).addOrderBy(new QueryOrderby("pubts", "desc"));
            List<CheckInventory_b> checkInventory_bs = MetaDaoHelper.queryObject(CheckInventory_b.ENTITY_NAME, querySchema, null);
            if (CollectionUtils.isNotEmpty(checkInventory_bs)) {
                CheckInventory_b checkInventory_b = checkInventory_bs.get(0);
                CheckInventory checkInventory = MetaDaoHelper.findById(CheckInventory.ENTITY_NAME, checkInventory_b.getMainid());
                return checkInventory;
            }
        } catch (Exception e) {
            log.error("getInventoryByCheckId exception", e);
        }
        return null;
    }


    @Override
    public List<String> getDeptListByStaffId(String staffId) {
        List<String> depts = new ArrayList<>();
        try {
            if (StringUtils.isNotBlank(staffId)) {
                List<Map<String, Object>> orgList = QueryBaseDocUtils.queryOrgByStaffId(staffId);
                for (Map<String, Object> map : orgList) {
                    Object dept_id = map.get("dept_id");
                    if (dept_id != null) {
                        depts.add(String.valueOf(dept_id));
                    }
                }
            }
        } catch (Exception e) {
            log.error("getDeptListByStaffId exception", e);
        }
        return depts;
    }

    public List<String> getDeptListByIdentityId(String identityId) {
        List<String> depts = new ArrayList<>();
        try {
            if (StringUtils.isNotBlank(identityId)) {
                User user = userService.find(Long.valueOf(identityId));
                Object hstaff = user.get("hstaff");
                if (hstaff != null) {
                    return getDeptListByStaffId(String.valueOf(hstaff));
                }
            }
        } catch (Exception e) {
            log.error("getDeptListByIdentityId exception", e);
        }
        return depts;
    }

    @Override
    public List<String> getDeptListByIdentityIdForMigration(String tenantId, String identityId) {
        List<String> depts = new ArrayList<>();
        try {
            if (StringUtils.isBlank(identityId) || StringUtils.isBlank(tenantId) || "~".equals(tenantId)) {
                return depts;
            }
            AtomicReference<List<String>> listAtomicReference = new AtomicReference<>(RobotExecutors.runAs(tenantId, () -> getDeptListByIdentityId(identityId)));
            depts = listAtomicReference.get();
        } catch (Exception e) {
            log.error("getDeptListByIdentityIdForMigration exception", e);
        }
        return depts;
    }

    public CheckStatus buildCheckStatus(CheckStock checkStock) {
        String department = null;
        String operator = InvocationInfoProxy.getIdentityId(); //身份id
        String staffId = InvocationInfoProxy.getUserid();
        try {
            User user = userService.find(Long.valueOf(InvocationInfoProxy.getIdentityId()));
            department = user.getDepartment();
            Object hstaff = user.get("hstaff");
            if (hstaff != null) {
                staffId = String.valueOf(hstaff);
            }
        } catch (Exception e) {
            log.error("查询用户部门失败", e);
        }
        CheckStatus checkStatus = new CheckStatus();
        checkStatus.setAccentity(checkStock.getAccentity());//会计主体
        checkStatus.setCheckBillDir(checkStock.getCheckBillDir());//支票方向
        checkStatus.setCheckBillNo(checkStock.getCheckBillNo());//支票编号
        checkStatus.setCheckBillType(checkStock.getCheckBillType());//支票类型
        checkStatus.setCheckId(checkStock.getId());
        checkStatus.setBeforeCheckBillStatus(null);//更新前支票状态
        checkStatus.setAfterCheckBillStatus(checkStock.getCheckBillStatus());//更新后支票状态
        Date operatorDate = new Date();
        if (CmpCheckStatus.InStock.getValue().equals(checkStock.getCheckBillStatus()) && checkStock.getBusiDate() != null) {
            operatorDate = checkStock.getBusiDate();
        }
        if (CmpCheckStatus.Disposal.getValue().equals(checkStock.getCheckBillStatus()) && checkStock.getVouchdate() != null) {
            operatorDate = checkStock.getVouchdate();
        }
        if (CmpCheckStatus.Cashed.getValue().equals(checkStock.getCheckBillStatus()) && checkStock.getCashDate() != null) {
            operatorDate = checkStock.getCashDate();
        }
        checkStatus.setOperator(operator);//操作人
        List<String> deptListByStaffId = getDeptListByStaffId(staffId);
        if (CollectionUtils.isNotEmpty(deptListByStaffId)) {
            department = Joiner.on(",").join(deptListByStaffId);
        }
        checkStatus.setOperatorDept(department);//操作人部门
        checkStatus.setOperatorDate(operatorDate);//操作日期
        checkStatus.setId(ymsOidGenerator.nextId());
        checkStatus.setEntityStatus(EntityStatus.Insert);
        return checkStatus;
    }

    @Override
    public void recordCheckStatusByMap(List<Map<String, Object>> checkStockList, String afterCheckBillStatus) throws Exception {
        List<CheckStock> checkStocks = new ArrayList<>();
        for (Map<String, Object> checkStock : checkStockList) {
            CheckStock checkStockNew = new CheckStock();
            checkStockNew.init(checkStock);
            checkStocks.add(checkStockNew);
        }
        recordCheckStatus(checkStocks, afterCheckBillStatus);
    }

    @Override
    public void recordCheckStatusByInventory(List<Map<String, Object>> checkStockList, String afterCheckBillStatus, Long mainid) throws Exception {
        List<CheckStock> checkStocks = new ArrayList<>();
        for (Map<String, Object> checkStock : checkStockList) {
            CheckStock checkStockNew = new CheckStock();
            checkStockNew.init(checkStock);
            checkStocks.add(checkStockNew);
        }
        recordCheckStatus(checkStocks, afterCheckBillStatus, mainid);
    }

    @Override
    public void recordCheckStatusByCheckId(Long checkId, String afterCheckBillStatus) throws Exception {
        CheckStock checkOne = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkId);
        CheckStatus checkStatus = buildCheckStatus(checkOne, afterCheckBillStatus, null, null);
        CmpMetaDaoHelper.insert(CheckStatus.ENTITY_NAME, checkStatus);
    }

    @Override
    public void recordCheckStatusByCheckStock(Long checkId, CheckStock check) throws Exception {
        CheckStock checkOne = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkId);
        checkOne.setDrawerDate(check.getDrawerDate());
        checkOne.setCashDate(check.getCashDate());
        CheckStatus checkStatus = buildCheckStatus(checkOne, check.getCheckBillStatus(), null, null);
        CmpMetaDaoHelper.insert(CheckStatus.ENTITY_NAME, checkStatus);
    }

    @Override
    public void recordCheckStatusByCheckDTO(Long checkId, CheckDTO check) throws Exception {
        CheckStock checkOne = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkId);
        checkOne.setDrawerDate(check.getDrawerDate());
        checkOne.setCashDate(check.getCashDate());
        CheckStatus checkStatus = buildCheckStatus(checkOne, check.getCheckBillStatus(), null, null);
        CmpMetaDaoHelper.insert(CheckStatus.ENTITY_NAME, checkStatus);
    }

    @Override
    public void recordCheckStatusByCancelCash(List<CheckStock> checkStocks) throws Exception {
        List<CheckStatus> checkStatusList = new ArrayList<>();
        for (CheckStock checkStock : checkStocks) {
            checkStatusList.add(buildCheckStatus(checkStock, checkStock.getBeforeCashStatus(), null, null));
        }
        CmpMetaDaoHelper.insert(CheckStatus.ENTITY_NAME, checkStatusList);
    }

    @Override
    public void recordCheckStatusByInsert(List<CheckStock> checkStocks) throws Exception {
        List<CheckStatus> checkStatusList = new ArrayList<>();
        for (CheckStock checkStock : checkStocks) {
            checkStatusList.add(buildCheckStatus(checkStock));
        }
        CmpMetaDaoHelper.insert(CheckStatus.ENTITY_NAME, checkStatusList);
    }

    //取消领用
    @Override
    public boolean isCancelUsed(CheckStatus checkStatus) {
        if (CmpCheckStatus.Use.getValue().equals(checkStatus.getBeforeCheckBillStatus()) && CmpCheckStatus.InStock.getValue().equals(checkStatus.getAfterCheckBillStatus())) {
            return true;
        }
        return false;
    }

    @Override
    public List<CheckStatus> getCheckStatusByAccAndOpDept(Set<String> acc, String opDept) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = new QueryConditionGroup();
        group.addCondition(QueryCondition.name("accentity").in(acc));//会计主体
        group.addCondition(QueryCondition.name("operatorDept").like(opDept));//操作人部门
        querySchema.addCondition(group);
        List<CheckStatus> checkStatuses = MetaDaoHelper.queryObject(CheckStatus.ENTITY_NAME, querySchema, null);
        return checkStatuses;
    }

    @Override
    public void recordCheckStatusForMigration(List<CheckStock> checkStocks, String beforeCheckBillStatus) throws Exception {
        List<CheckStatus> checkStatus = new ArrayList<>();
        for (CheckStock checkStock : checkStocks) {
            checkStatus.add(buildCheckStatusForMigration(checkStock, beforeCheckBillStatus));
        }
        CmpMetaDaoHelper.insert(CheckStatus.ENTITY_NAME, checkStatus);
    }

    @Override
    public void recordInStockCheckStatusForMigration(List<CheckStock> checkStocks, String beforeCheckBillStatus) throws Exception {
        List<CheckStatus> checkStatus = new ArrayList<>();
        for (CheckStock checkStock : checkStocks) {
            checkStatus.add(buildInStockCheckStatusForMigration(checkStock, beforeCheckBillStatus));
        }
        CmpMetaDaoHelper.insert(CheckStatus.ENTITY_NAME, checkStatus);
    }

    /**
     * 新增处置中时候，可能处置主表还没有插入数据，所以参数传入
     *
     * @param checkId
     * @param checkBillStatus
     * @param checkManage
     * @throws Exception
     */
    @Override
    public void recordCheckStatusByManage(Long checkId, String checkBillStatus, CheckManage checkManage) throws Exception {
        CheckStock checkOne = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, checkId);
        CheckStatus checkStatus = buildCheckStatus(checkOne, checkBillStatus, null, checkManage);
        CmpMetaDaoHelper.insert(CheckStatus.ENTITY_NAME, checkStatus);
    }
}
