package com.yonyoucloud.fi.cmp.constant;

/**
 *
 * 跨行标识常量类
 * 01:本地本行；02:异地本行；03:本地他行；04:异地他行；05:国外他行；06: 国外本行
 * @author maliangn
 */
public interface CrossBankType {
    //本地本行
    String LL = "01";
    //异地本行
    String OL = "02";
    //本地他行
    String LO = "03";
    //异地他行
    String OO = "04";
    //05:国外他行
    String FO = "05";
    //06:国外本行
    String FL = "06";
    //银企联判断
    String UN = "99";

}
