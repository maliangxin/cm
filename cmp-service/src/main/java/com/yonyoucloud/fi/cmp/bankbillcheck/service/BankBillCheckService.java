package com.yonyoucloud.fi.cmp.bankbillcheck.service;

import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.List;
import java.util.Map;

/**
 * @Author zhucongcong
 * @Date 2024/9/25
 */
public interface BankBillCheckService {

    List<String> queryBillInfo(CtmJSONObject params) throws Exception;

    CtmJSONObject match( CtmJSONArray rows) throws Exception;

    CtmJSONObject unMatch(CtmJSONArray rows) throws Exception;

    CtmJSONObject unMatchUpdate(CtmJSONObject params) throws Exception;

    CtmJSONObject checkResultSubmit(CtmJSONArray jsonArray) throws Exception;

    CtmJSONObject checkResultQuery(CtmJSONArray jsonArray) throws Exception;

    Map<String, Object> scheduleQueryBillInfo(CtmJSONObject params) throws Exception;

    Map<String, Object> scheduleCheckResultQuery(CtmJSONObject params) throws Exception;
}
