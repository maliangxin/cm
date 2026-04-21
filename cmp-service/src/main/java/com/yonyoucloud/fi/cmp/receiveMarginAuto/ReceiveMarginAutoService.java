package com.yonyoucloud.fi.cmp.receiveMarginAuto;

import java.util.Map;

public interface ReceiveMarginAutoService {

    Map<String,Object> receiveMarginAutoTask(int beforeDays, String logId, String tenant,String accentity);


}

