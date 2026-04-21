package com.yonyoucloud.fi.cmp.event.vo;

import lombok.Data;

@Data
public class EventInfoFetch {

    String classifier;
    String srcBusiId;
    String busiFetchBatchId;
    String busiFetchIndex;
    String busiFetchITotal;

}
