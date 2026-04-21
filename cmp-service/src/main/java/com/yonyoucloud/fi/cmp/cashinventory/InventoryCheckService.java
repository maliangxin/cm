package com.yonyoucloud.fi.cmp.cashinventory;

import org.imeta.orm.base.BizObject;

public interface InventoryCheckService {

    Boolean ruleCheck(BizObject bill,String type) throws Exception;

    void amountCalculation(BizObject bill) throws Exception;

}
