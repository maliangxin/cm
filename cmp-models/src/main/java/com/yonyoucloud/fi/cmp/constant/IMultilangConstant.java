package com.yonyoucloud.fi.cmp.constant;

import com.yonyoucloud.fi.cmp.common.CtmException;

public class IMultilangConstant {

   public static CtmException noRateError = new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000DF", "未取到汇率"));

   public static String noRateStringError = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_211EE3880468000C", "未获取到汇率类型为[%s]的[%s]到[%s]的汇率值，请检查汇率配置！");

}
