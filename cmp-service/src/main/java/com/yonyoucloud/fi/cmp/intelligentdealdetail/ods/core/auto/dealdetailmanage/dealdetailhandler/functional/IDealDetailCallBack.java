package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.dealdetailhandler.functional;
import java.io.Serializable;
/**
 * @Author guoyangy
 * @Date 2024/7/8 14:47
 * @Description todo
 * @Version 1.0
 */
@FunctionalInterface
public interface IDealDetailCallBack extends Serializable {
    void  call() throws Exception;
}