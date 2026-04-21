package com.yonyoucloud.fi.cmp.util.threadpool;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: liaojbo
 * @Date: 2025年03月12日 15:56
 * @Description:
 */
@Data
public class ThreadResult {
    List sucessReturnList = new ArrayList<>();
    List errorReturnList = new ArrayList<>();

}
