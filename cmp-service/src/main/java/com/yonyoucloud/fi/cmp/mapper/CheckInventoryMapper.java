package com.yonyoucloud.fi.cmp.mapper;

import com.yonyoucloud.fi.cmp.vo.checkstock.CheckInventoryDTO;
import com.yonyoucloud.fi.cmp.vo.checkstock.CheckManageVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface CheckInventoryMapper {

    CheckInventoryDTO queryCheckInventoryByCheckId(@Param("checkBillNo") String checkBillNo, @Param("checkId") Long checkId);
}
