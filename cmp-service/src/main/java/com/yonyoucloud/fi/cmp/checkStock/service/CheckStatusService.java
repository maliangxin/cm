package com.yonyoucloud.fi.cmp.checkStock.service;

import com.yonyoucloud.fi.cmp.checkmanage.CheckManage;
import com.yonyoucloud.fi.cmp.checkstock.CheckStatus;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.vo.checkstock.CheckDTO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 支票状态 *
 */
public interface CheckStatusService {

    public void recordCheckStatus(List<CheckStock> checkStocks, String afterCheckBillStatus) throws Exception;

    public void recordCheckStatusByMap(List<Map<String, Object>> checkStockList, String afterCheckBillStatus) throws Exception;

    public void recordCheckStatusByInventory(List<Map<String, Object>> checkStockList, String afterCheckBillStatus, Long mainid) throws Exception;

    public void recordCheckStatusByCheckId(Long checkId, String afterCheckBillStatus) throws Exception;

    public void recordCheckStatusByCheckDTO(Long checkId, CheckDTO check) throws Exception;

    public void recordCheckStatusByCheckStock(Long checkId, CheckStock check) throws Exception;

    public void recordCheckStatusByCancelCash(List<CheckStock> checkStocks) throws Exception;

    public void recordCheckStatusByInsert(List<CheckStock> checkStocks) throws Exception;

    public boolean isCancelUsed(CheckStatus checkStatus);

    public List<CheckStatus> getCheckStatusByAccAndOpDept(Set<String> acc, String opDept) throws Exception;

    public List<String> getDeptListByStaffId(String staffId);

    public List<String> getDeptListByIdentityIdForMigration(String tenantId, String staffId);

    public void recordCheckStatusForMigration(List<CheckStock> checkStocks, String beforeCheckBillStatus) throws Exception;
    public void recordInStockCheckStatusForMigration(List<CheckStock> checkStocks, String beforeCheckBillStatus) throws Exception;
    void recordCheckStatusByManage(Long checkId, String checkBillStatus, CheckManage checkManage) throws Exception;

    CheckStatus buildCheckStatus(CheckStock checkStock, String afterCheckBillStatus, Long maiid, CheckManage checkManage) throws Exception;
}
