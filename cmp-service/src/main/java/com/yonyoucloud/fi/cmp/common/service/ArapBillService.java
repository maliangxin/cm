package com.yonyoucloud.fi.cmp.common.service;

import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import org.imeta.orm.base.BizObject;

/**
 * @author maliang
 * @version V1.0
 * @date 2021/7/29 16:59
 * @Copyright yonyou
 *
 */
public interface ArapBillService {

    BizObject beforeSaveBillToCmp(BillDataDto billDataDto);

    void afterSaveBillToCmp(BillContext billContext, BizObject bizObject);

    boolean checkSettleMode(Object settlemode);

    boolean checkLisenceValid() throws Exception;

    void checkService(String billnum) throws Exception;

}
