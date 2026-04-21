package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.consts;

import java.util.Arrays;
import java.util.List;

/**
 * @Author guoyangy
 * @Date 2024/2/28 16:30
 * @Description todo
 * @Version 1.0
 */
public class BankDealDetailConst {
    // yms
    public static final List<String> YQL_ALL_FIELDS = Arrays.asList("tran_date", "tran_time", "dc_flag", "bank_seq_no", "to_acct_no", "to_acct_name", "to_acct_bank", "to_acct_bank_name", "curr_code", "cash_flag", "acct_bal", "tran_amt", "oper", "value_date", "use_name", "remark", "bank_reconciliation_code", "unique_no", "detail_check_id", "bank_check_code", "rate", "fee_amt", "fee_amt_cur", "remark01", "pay_use_desc", "corr_fee_amt", "corr_fee_amt_cur", "sub_name", "sub_code", "proj_name", "budget_source", "voucher_type", "voucher_no", "mdcard_no", "mdcard_name", "payment_manage_type", "eco_class", "budget_relevance_no", "add_amt", "balance", "is_refund", "refund_original_transaction");

    public static final String NORMALSTREAM="normalStream";
    public static final String REPEATSTREAM="repeatStream";


    public static final int CODE_SUCC=1;
    public static final int CODE_FAIL=0;
    public static final String SERVICE_RESP_CODE="000000";
    public static final String SERVICE_STATUS="00";
    public static final String SEPERATOR = "_";
    // 疑重过滤器使用的查询数据的天数
    public static final int SELECT_DAY_COUNT = 35;
    // 疑重四要素字段
    public static final String REPEATFACTIRS = "bankaccount,tran_date,tran_amt,dc_flag";
    // 疑重四要素
    public static final int REPEAT_FACTIRS_4 = 0;
    // 疑重自定义
    public static final int REPEAT_FACTIRS_DEFINE = 1;

    // 疑似重复初始
    public static final int REPEAT_INIT = 0;
    // 疑似重复
    public static final int REPEAT_DOUBT = 1;
    // 疑重确认重复
    public static final int REPEAT_CONFIRM = 2;
    // 疑重确认正常
    public static final int REPEAT_NORMAL = 3;



    public enum RequestTypeEnum {
        REQUESTTYPE_CHECK(1,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540071C", "手动点击") /* "手动点击" */),
        REQUESTTYPE_SCHEDULED(2,com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540071E", "调度任务") /* "调度任务" */)
        ;
        private int code;
        private String desc;
        RequestTypeEnum(int code,String desc){
            this.code = code;
            this.desc = desc;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }
    }

    public enum TranCodeEnum {
        TRANCODE_DEALDETAIL("40T22",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400718", "交易明细") /* "交易明细" */),
        TRANCODE_HISTORYBALANCE("40T21",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400719", "历史余额") /* "历史余额" */),
        TRANCODE_CURRBALANCE("40T20",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540071A", "实时余额") /* "实时余额" */),
        TRANCODE_RECEIPTQUERY("40T23",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540071B", "回单查询") /* "回单查询" */),
        TRANCODE_RECEIPTDOWNLOAD("48T23",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540071D", "回单下载") /* "回单下载" */),
        ;
        private String code;
        private String desc;
        TranCodeEnum(String code,String desc){
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }
    }


}
