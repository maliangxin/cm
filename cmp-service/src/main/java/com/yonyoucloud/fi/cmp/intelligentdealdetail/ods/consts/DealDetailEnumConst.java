package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts;

/**
 * @Author guoyangy
 * @Date 2024/6/28 9:14
 * @Description todo
 * @Version 1.0
 */
public class DealDetailEnumConst {
    public static final Short  APPLY_OBJECT_BANKRECONCILIATION = 1;

    //收付单据匹配关联规则分类,财资统一对账码、银行对账编码类别
    public static final String  PAYMENT_RULE_SMARTCHECKNO = "1";
    //收付单据匹配关联规则分类，要素类别
    public static final String  PAYMENT_RULE_ELEMENT = "2";
    //逻辑数据池启用状态
    public static final String  CDP_ENABLED = "1";
    //收付单据匹配-自动辨识匹配规则开
    public static final String  IDENTIFY_ENABLED = "1";
    public static final Short  IDENTIFY_B_ENABLED = 1;
    public static final String  IDENTIFY_SYSTEM_0511 = "system0511";
    public static final String  IDENTIFY_SYSTEM_0512 = "system0512";
    public static final String  IDENTIFY_SYSTEM_0521 = "system0521";
    public static final String  IDENTIFY_SYSTEM_0522 = "system0522";
    // [flow_type 1收付单据关联]
    public static final String  FLOWTYPE = "1";
    //[enable 1开启]
    public static final String  FLOW_ENABLED = "1";
    //[object:1流水关联]
    public static final String  FLOW_OBJECT = "1";
    //[association_mode 1:自动关联]
    public static final String  ASSOCIATION_MODE = "1";
    //流水处理规则，自动关联，全部需要确认
    public static final int ARTICONFIRM =1;

    public static final Integer RELATIONSTATUS_SUCC=2;

    public static final String RELATION_CONFIRM_SYNC="sync";
    public static final String RELATION_CONFIRM_ASYNC="async";
    //自动辨识匹配预置死规则0：统一财资对账码 1：银行对账码
    public static final Integer  IDENTIFY_SMARTCHECKNO = 0;
    public static final Integer  IDENTIFY_BANKCHECKNO = 1;

    public static final String  PAYCODE = "system0103";
    public static final String RULEPARAM="rules";
    public static final String ODSPARAM="odsparam";
    public static final String BANKRECONCILIATION_CALLACK="bankReconciliatioCallback";
    public static final String ODSID ="odsId";
    public static final String EXECUTERESULTDESC = "executeResult";
    public static final String EXECUTESTATUS = "executeStatus";
    public static final String JDBCAPIDAO = "odsBaseDAO";
    public static final int BUSINESSTYPE_MATCH=1;
    public static final int BUSINESSTYPE_PROCESS=2;
    public static final String LOGICDATASOURCE = "yonbip-fi-ctmcmp_dataSource";
    public static final String MODULE = "yonbip-fi-ctmcmp";
    public static final int PAGESIZE = 50;
    public static final int MAXLENGTH = 8000;
    //消费端线程池最大线程数
    public static final int MAXPOOL = 10;
    public static final String ODSEVENTBUS = "ods_eventbus";
    public static final String SAVE_DIRECT="saveDirect";
    public static final String SAVE_DIRECT_FINISH="saveDirectFinish";
    public static final String SAVE_AND_UPDATE_NOT_FINISH="checkDirectOnly";
    public enum DealDetailProcessStatusEnum{
        DEALDETAIL_MATCH_NO_START((short)1,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007D7", "辨识匹配未开始") /* "辨识匹配未开始" */),
        DEALDETAIL_MATCH_START((short)2,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007D8", "开始辨识匹配") /* "开始辨识匹配" */),
        DEALDETAIL_MATCH_PENDING((short)3,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007D9", "辨识匹配挂起") /* "辨识匹配挂起" */),
        DEALDETAIL_MATCH_ERROR((short)4,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007DA", "辨识匹配非业务异常失败") /* "辨识匹配非业务异常失败" */),
        DEALDETAIL_MATCH_RECEIVEPAY_SUCC((short)5,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007DB", "收付单据辨识匹配成功") /* "收付单据辨识匹配成功" */),
        DEALDETAIL_MATCH_GENERATE_SUCC((short)6,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007DC", "生单类型辨识匹配成功") /* "生单类型辨识匹配成功" */),
        DEALDETAIL_MATCH_SUCC((short)7,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007DD", "辨识匹配成功") /* "辨识匹配成功" */),
//        DEALDETAIL_PROCESS_RELATED_NO_START((short)8,"业务关联流程未开始处理"),
        DEALDETAIL_PROCESS_RELATED_START((short)9,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007DE", "业务关联流程开始处理") /* "业务关联流程开始处理" */),
        DEALDETAIL_PROCESS_RELATED_PENDING((short)10,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007DF", "业务关联流程挂起") /* "业务关联流程挂起" */),
        DEALDETAIL_PROCESS_RELATED_FAIL((short)11,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007E0", "业务关联流程处理失败") /* "业务关联流程处理失败" */),
//        DEALDETAIL_PROCESS_CREDENTIALT_NO_START((short)12,"业务凭据流程未开始处理"),
        DEALDETAIL_PROCESS_CREDENTIALT_START((short)13,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007E1", "业务凭据流程开始处理") /* "业务凭据流程开始处理" */),
        DEALDETAIL_PROCESS_CREDENTIALT_PENDING((short)14,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007E2", "业务凭据流程挂起") /* "业务凭据流程挂起" */),
        DEALDETAIL_PROCESS_CREDENTIALT_FAIL((short)15,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007E3", "业务凭据流程处理失败") /* "业务凭据流程处理失败" */),
//        DEALDETAIL_PROCESS_GENERATEBILL_NO_START((short)16,"生单流程未开始处理"),
        DEALDETAIL_PROCESS_GENERATEBILL_START((short)17,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007E6", "生单流程开始处理") /* "生单流程开始处理" */),
        DEALDETAIL_PROCESS_GENERATEBILL_PENDING((short)18,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007E8", "生单流程挂起") /* "生单流程挂起" */),
        DEALDETAIL_PROCESS_GENERATEBILL_FAIL((short)19,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007EA", "生单流程处理失败") /* "生单流程处理失败" */),
//        DEALDETAIL_PROCESS_PUBLISH_NO_START((short)20,"发布认领流流程未开始处理"),
        DEALDETAIL_PROCESS_PUBLISH_START((short)21,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007EB", "发布认领流流程开始处理") /* "发布认领流流程开始处理" */),
        DEALDETAIL_PROCESS_PUBLISH_FAIL((short)23,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007EC", "发布认领流程处理失败") /* "发布认领流程处理失败" */),
        //发布是非终态===发布+未匹配到任何规则的数据
        DEALDETAIL_PROCESS_SUCC((short)24,  com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007ED", "非终态：流水自动处理已完成,待手工处理") /* "非终态：流水自动处理已完成,待手工处理" */),
        DEALDETAIL_PROCESS_FINISH((short)25,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007EE", "终态：流水自动处理已完成,自动处理成功") /* "终态：流水自动处理已完成,自动处理成功" */),
        ;
        private Short status;
        private String desc;
        DealDetailProcessStatusEnum(Short status,String desc){
            this.status=status;
            this.desc=desc;
        }
        public Short getStatus() {
            return status;
        }
        public String getDesc() {
            return desc;
        }
    }
    public enum ExecuteStatusEnum{
        EXECUTE_STATUS_SUCCESS_END("1",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007F2", "阻断性规则且满足阻断条件，下一步执行具体流程处理") /* "阻断性规则且满足阻断条件，下一步执行具体流程处理" */),
        EXECUTE_STATUS_SUCCESS_PENDING("2",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007F4", "需要人工介入") /* "需要人工介入" */),
        EXECUTE_STATUS_SUCCESS_CONTINUE("3",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007F6", "本规则顺利执行完成，可以执行下一个规则") /* "本规则顺利执行完成，可以执行下一个规则" */),
        EXECUTE_STATUS_SYSTEM_ERROR("4",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007F8", "非业务异常，比如空指针、超时、数据库操作失败等") /* "非业务异常，比如空指针、超时、数据库操作失败等" */),
        ;
        private String status;
        private String desc;
        ExecuteStatusEnum(String status,String desc){
            this.status=status;
            this.desc=desc;
        }
        public String getStatus() {
            return status;
        }
        public String getDesc() {
            return desc;
        }
    }
    public enum AccessChannelEnum{
        YQL(1,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007FE", "银企联接入") /* "银企联接入" */),
        MANUAL(2,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007FF", "手工执行") /* "手工执行" */),
        SIGNAL(3,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400800", "单笔导入或新增") /* "单笔导入或新增" */),
        NOTICE(4,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400801", "到账通知或则openapi") /* "到账通知或则openapi" */),
        ;
        private int key;
        private String desc;
        AccessChannelEnum(int key,String desc){
            this.key=key;
            this.desc=desc;
        }
        public int getKey() {
            return key;
        }
        public String getDesc() {
            return desc;
        }
    }
    public enum Deduplication_KEYEnum{
        Deduplication_KEY_REPEAT("1",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007E4", "流水在业务表已存在,防重") /* "流水在业务表已存在,防重" */),
        Deduplication_KEY_ADD("2",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007E5", "流水在业务表不存在，新增") /* "流水在业务表不存在，新增" */),
        Deduplication_KEY_UPDATE("3",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007E7", "流水在业务表已存在,更新") /* "流水在业务表已存在,更新" */),
        Deduplication_KEY_ROLLBACK("4",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007E9", "流水去重处理异常，ods回滚到初始态") /* "流水去重处理异常，ods回滚到初始态" */),
        ;
        private String key;
        private String desc;
        Deduplication_KEYEnum(String key,String desc){
            this.key=key;
            this.desc=desc;
        }
        public String getKey() {
            return key;
        }
        public String getDesc() {
            return desc;
        }
    }
    /**
     * ods表流水处理状态processstatus枚举值
     *
     * */
    public enum ODS_processstatusEnum{
        PROCESSSTATUS_NO(1,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007EF", "未处理") /* "未处理" */),
        PROCESSSTATUS_START(2,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007F0", "开始处理") /* "开始处理" */),
        PROCESSSTATUS_NOTHING(3,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007F1", "业务去重已完成无需后续处理") /* "业务去重已完成无需后续处理" */),
        PROCESSSTATUS_REPEAT_ERROR(4,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007F3", "业务去重异常") /* "业务去重异常" */),
        PROCESSSTATUS_REPEAT_FINISH_WAIT_MATCH(5,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007F5", "已完成业务去重待辨识匹配") /* "已完成业务去重待辨识匹配" */),
        PROCESSSTATUS_SUSPEND(6,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007F7", "已挂起") /* "已挂起" */),
        PROCESSSTATUS_MATCHSUCC(7,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007F9", "辨识匹配处理成功") /* "辨识匹配处理成功" */),
        PROCESSSTATUS_MATCHFAIL(8,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007FA", "辨识匹配处理失败") /* "辨识匹配处理失败" */),
        PROCESSSTATUS_PROCESS_START(9,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007FB", "开始流程处理") /* "开始流程处理" */),
        PROCESSSTATUS_PROCESSFAIL(10,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007FC", "流程处理失败") /* "流程处理失败" */),
        PROCESSSTATUS_PROCESSSUCC(11,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007FD", "流程处理成功") /* "流程处理成功" */),
        ;
        private int processstatus;
        private String desc;
          ODS_processstatusEnum(int processstatus,String desc){
            this.processstatus = processstatus;
            this.desc=desc;
        }
        public int getProcessstatus() {
            return processstatus;
        }
        public String getDesc() {
            return desc;
        }
    }
}
