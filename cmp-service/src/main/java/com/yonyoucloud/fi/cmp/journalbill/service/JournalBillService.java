package com.yonyoucloud.fi.cmp.journalbill.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

public interface JournalBillService {

    CtmJSONObject batchSaveOrUpdateForOpenApi(CtmJSONObject param) throws Exception;

    String batchDeleteForOpenApi(CtmJSONObject param) throws Exception;

    CtmJSONObject checkAddTransType(CtmJSONObject params) throws Exception;

}
