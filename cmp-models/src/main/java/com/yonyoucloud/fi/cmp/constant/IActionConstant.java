package com.yonyoucloud.fi.cmp.constant;

/**
 * 动作常量
 * @author maliang
 * @version V1.0
 * @date 2021/6/22 14:56
 * @Copyright yonyou
 */
public interface IActionConstant {

    //保存
    String SAVE = "save";

    // 调用收付结算动作
    String SETTLEACTION = "arapSettle";

    // 调用收付取消结算动作
    String UNSETTLEACTION = "arapUnSettle";

    // 自动生单同步应收动作
    String AUTOBILLSAVE ="autoBillSave";


    // 自动生单删除单据动作
    String AUTOBILLDELETE = "autobilldelete";

}
