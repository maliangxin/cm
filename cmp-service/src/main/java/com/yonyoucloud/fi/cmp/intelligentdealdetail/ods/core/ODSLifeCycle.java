package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core;
/**
 * ods流水生命周期
 * */
public interface ODSLifeCycle {
    default void start(){}
    default void stop(){}
    default boolean isRunning(){return false;}
}