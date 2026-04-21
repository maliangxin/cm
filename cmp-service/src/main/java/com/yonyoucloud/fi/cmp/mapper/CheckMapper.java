package com.yonyoucloud.fi.cmp.mapper;

import com.yonyoucloud.fi.cmp.vo.checkstock.CheckDTO;
import com.yonyoucloud.fi.cmp.vo.checkstock.CheckStatusDTO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CheckMapper {

    List<CheckDTO> queryCheckStocksByPage(@Param("start") int start, @Param("pageSize") int pageSize);

    List<CheckDTO> queryCheckStocksAfterInStockByPage(@Param("start") int start, @Param("pageSize") int pageSize);

    List<CheckDTO> queryCheckStocks();

    List<CheckDTO> queryCheckStocksAfterInStock();

    int queryCheckStocksCount();

    int queryCheckStocksAfterInStockCount();

    void deleteStatusByBeforeStatus(String beforeCheckBillStatus);

    void batchInsertStatus(List<CheckStatusDTO> checkStatus);

    void batchInsertStatusWithPubts(List<CheckStatusDTO> checkStatus);

    int selectStatusByBeforeStatus(String beforeCheckBillStatus);
}
