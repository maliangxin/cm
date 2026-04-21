package com.yonyoucloud.fi.cmp.util.process;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@Builder
@NoArgsConstructor      // ← 给 Jackson 用
@AllArgsConstructor
public class ProcessInfo {

    AtomicInteger count;
    AtomicInteger sucessCount;
    AtomicInteger failCount;
    int totalCount;//总数
    CopyOnWriteArrayList<String> messages;
    CopyOnWriteArrayList<String> infos;
    CopyOnWriteArrayList<String> failInfos;
    CopyOnWriteArrayList<String> noDataInfos;

}
