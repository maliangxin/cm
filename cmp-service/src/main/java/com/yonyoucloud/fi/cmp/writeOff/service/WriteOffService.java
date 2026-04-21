package com.yonyoucloud.fi.cmp.writeOff.service;

import java.util.Map;

public interface WriteOffService {

    Map<String,Object> WriteOffTask(int beforeDays, String logId, String tenant);


}

