package com.yonyoucloud.fi.cmp.paymentbill.service.redis;

import lombok.Data;

import java.util.List;

@Data
public class ProcessVoInner {

    int count;
    int failCount;
    List<Object> infos;
    List<String> messages;
    int sucessCount;

}
