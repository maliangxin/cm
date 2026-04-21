package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.auto.dealdetailmanage.ruleconst;
import com.yonyoucloud.fi.cmp.util.StringUtils;

import java.util.Arrays;
import java.util.List;
/**
 * @Author guoyangy
 * @Date 2024/7/3 13:59
 * @Description todo
 * @Version 1.0
 */
public class RuleCodeConst {
   /**
    * 辨识匹配规则编码
    * */
    public static final String  SYSTEM001="system001";
    public static final String  SYSTEM002="system002";
    public static final String  SYSTEM003="system003";
    public static final String  SYSTEM004="system004";
    public static final String  SYSTEM005="system005";
    public static final String  SYSTEM006="system006";
    public static final String  SYSTEM007="system007";
    public static final String  SYSTEM008="system008";
    public static final String  SYSTEM009="system009";
    public static final String  SYSTEM010="system010";
    /**
     * 流程处理编码
     * */
    public static final String  SYSTEM021="system021";
    public static final String  SYSTEM022="system022";
    //单据匹配生单
    public static final String  SYSTEM023="system023";
    public static final List<String> ALLMATCHRULE = Arrays.asList(SYSTEM001,SYSTEM002,SYSTEM003,SYSTEM004,SYSTEM005,SYSTEM006,SYSTEM007,SYSTEM008,SYSTEM009,SYSTEM010);
    public static final List<String> ALLPROCESSRULE = Arrays.asList(RuleCodeConst.SYSTEM021,RuleCodeConst.SYSTEM022,RuleCodeConst.SYSTEM023);
    public static final List<String> RULENAMELIST = Arrays.asList(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007C9", "收付单据关联流程") /* "收付单据关联流程" */,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007CB", "业务凭据关联流程") /* "业务凭据关联流程" */,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007CA", "生单流程") /* "生单流程" */,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007C8", "发布认领中心流程") /* "发布认领中心流程" */);

    public static final String  OPERTYPE_MATCH="matchDealDetailResultDaoImpl";
    public static final String  OPERTYPE_PROCESS="processDealDetailResultDaoImpl";

    public static String getOperType(String code){
        if(StringUtils.isEmpty(code)||ALLPROCESSRULE.contains(code)){
            return OPERTYPE_PROCESS;
        }
        if(ALLMATCHRULE.contains(code)){
            return OPERTYPE_MATCH;
        }
        return OPERTYPE_PROCESS;
    }

    public static String getRuleName(String code){
        if(code == null){
         return null;
        }
        if(SYSTEM001.equals(code)){
            return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007CE", "银行交易回单规则") /* "银行交易回单规则" */;
        }
       if(SYSTEM002.equals(code)){
        return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007D0", "银行退票匹配") /* "银行退票匹配" */;
       }
      if(SYSTEM003.equals(code)){
       return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007D2", "本方信息匹配") /* "本方信息匹配" */;
      }
      if(SYSTEM004.equals(code)){
       return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007C6", "对方类型辨识") /* "对方类型辨识" */;
      }
      if(SYSTEM005.equals(code)){
       return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007CC", "收付单据匹配") /* "收付单据匹配" */;
      }
      if(SYSTEM006.equals(code)){
       return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007CD", "业务凭据匹配") /* "业务凭据匹配" */;
      }
      if(SYSTEM007.equals(code)){
       return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007CF", "生单类型辨识") /* "生单类型辨识" */;
      }
      if(SYSTEM008.equals(code)){
       return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007D1", "发布对象辨识") /* "发布对象辨识" */;
      }
     if(SYSTEM009.equals(code)){
      return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007D3", "挂账辨识") /* "挂账辨识" */;
     }
     if(SYSTEM010.equals(code)){
      return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007C7", "其他信息辨识") /* "其他信息辨识" */;
     }
    if(SYSTEM021.equals(code)){
        return RULENAMELIST.get(0);
    }
    if(SYSTEM022.equals(code)){
        return RULENAMELIST.get(1);
    }
    if(SYSTEM023.equals(code)){
        return RULENAMELIST.get(2);
    }
     return null;
    }
}
