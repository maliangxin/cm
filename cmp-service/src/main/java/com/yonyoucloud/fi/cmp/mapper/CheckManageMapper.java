package com.yonyoucloud.fi.cmp.mapper;

import com.yonyoucloud.fi.cmp.vo.checkstock.CheckManageVO;
import org.springframework.stereotype.Repository;


@Repository
public interface CheckManageMapper {

    CheckManageVO queryCheckManageByCheckId(Long checkId);
}
