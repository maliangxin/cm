package com.yonyoucloud.fi.cmp.bankreceipt.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

public interface BankCheckMatchService {
     CtmJSONObject automatch(CtmJSONObject params) throws Exception;

     CtmJSONObject manualmatch(CtmJSONObject params);

     CtmJSONObject cancelmatch(CtmJSONObject params);
}
