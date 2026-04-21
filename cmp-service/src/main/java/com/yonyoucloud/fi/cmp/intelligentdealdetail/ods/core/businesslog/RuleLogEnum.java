package com.yonyoucloud.fi.cmp.intelligentdealdetail.ods.core.businesslog;

public class RuleLogEnum {

    public enum RuleLogProcess{
        BANK_RECEIPT_MODULE("00000","CmpRuleModuleLog"),
        BANK_RECEIPT_LOG_NAME("00001",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003C1", "银行流水规则辨识") /* "银行流水规则辨识" */),
        BANK_RECEIPT_ASSOCIATION_NAME("00101",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003C2", "银行交易回单匹配") /* "银行交易回单匹配" */),
        BANK_RECEIPT_ASSOCIATION_START("00102",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003C3", "开始执行回单辨识规则：") /* "开始执行回单辨识规则：" */),
        BANK_RECEIPT_ASSOCIATION_EXECUTION_STEP_ONE("00103",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003C4", "已经进行回单关联") /* "已经进行回单关联" */),
        BANK_RECEIPT_ASSOCIATION_EXECUTION_STEP_TWO("00104",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003C5", "设置回单关联状态：自动关联") /* "设置回单关联状态：自动关联" */),
        BANK_RECEIPT_ASSOCIATION_EXECUTION_STEP_Three("00105",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003C6", "银行流水关联回单成功！") /* "银行流水关联回单成功！" */),
        BANK_RECEIPT_ASSOCIATION_EXECUTION_STEP_FOUR("00106",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003C7", "银行流水未查询到回单信息，没有进行回单关联！") /* "银行流水未查询到回单信息，没有进行回单关联！" */),
        BANK_RECEIPT_LOG_PROCESS("00107",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003C8", "银行流水流程执行") /* "银行流水流程执行" */),
        BANK_RECEIPT_LOG_PROCESS_IDENTIFY_ONE("00108",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003C9", "银行流水生单流程") /* "银行流水生单流程" */),


        BANK_REFUND_NAME("00201",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003CA", "银行退票匹配") /* "银行退票匹配" */),
        BANK_REFUND_START("00202",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003CB", "开始执行银行退票规则:") /* "开始执行银行退票规则:" */),
        BANK_REFUND_STEP_ONE("00203",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003CC", "流水开始做流水退票辨识匹配(内存流水互相做疑似退票逻辑处理)") /* "流水开始做流水退票辨识匹配(内存流水互相做疑似退票逻辑处理)" */),
        BANK_REFUND_STEP_TWO("00204",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003CD", "匹配疑似退票时，设置：流水处理状态：辨识匹配挂起；设置退票状态：疑似退票") /* "匹配疑似退票时，设置：流水处理状态：辨识匹配挂起；设置退票状态：疑似退票" */),
        BANK_REFUND_STEP_THREE("00205",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003CE", "流水开始做流水退票辨识匹配(流水与数据库流水做疑似退票逻辑处理)") /* "流水开始做流水退票辨识匹配(流水与数据库流水做疑似退票逻辑处理)" */),

        GENERATE_BILL_module_NAME("00300",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003CF", "生单类型辨识") /* "生单类型辨识" */),
        GENERATE_BILL_MODULE_NAME("00301",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003D0", "相关性规则辨识") /* "相关性规则辨识" */),
        GENERATE_BILL_TYPE_NAME("00302",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003D1", "生单相关性规则辨识") /* "生单相关性规则辨识" */),
        GENERATE_BILL_PROCESS_NAME("00303",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003D2", "生单流程处理") /* "生单流程处理" */),
        GENERATE_BILL_TYPE_START("00304",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003D3", "开始执行生单辨识规则:") /* "开始执行生单辨识规则:" */),
        GENERATE_BILL_TYPE_ONE("00305",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003D4", "执行生单辨识规则完成，匹配结果：已经匹配上:") /* "执行生单辨识规则完成，匹配结果：已经匹配上:" */),
        GENERATE_BILL_TYPE_TWO("00306",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003D5", "执行生单辨识规则完成，匹配结果：未匹配上:") /* "执行生单辨识规则完成，匹配结果：未匹配上:" */),
        GENERATE_BILL_TYPE_THREE("00307",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003D6", "银行流水已匹配上生单规则辨识，但是生单辨识规则没有赋值生单类型，所以不会进行生单，单据继续执行后续操作！") /* "银行流水已匹配上生单规则辨识，但是生单辨识规则没有赋值生单类型，所以不会进行生单，单据继续执行后续操作！" */),


        OPPOSITE_TYPE_NAME("00401",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003D7", "对方信息匹配") /* "对方信息匹配" */),
        OPPOSITE_TYPE_START("00402",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003D8", "开始执行对方信息辨识规则：") /* "开始执行对方信息辨识规则：" */),
        OPPOSITE_TYPE_START_CUSTOMER("00403",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003D9", "开始执行对方信息辨识规则,进行【客户】辨识：") /* "开始执行对方信息辨识规则,进行【客户】辨识：" */),
        OPPOSITE_TYPE_START_CUSTOMER_RESULT("00404",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003DA", "对方信息辨识规则,辨识【客户】完成，通过[账户+名称]匹配结果：设置对方类型为【客户】,已匹配:") /* "对方信息辨识规则,辨识【客户】完成，通过[账户+名称]匹配结果：设置对方类型为【客户】,已匹配:" */),
        OPPOSITE_TYPE_START_CUSTOMER_RESULT_NO("00405",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003DB", "对方信息辨识规则,辨识【客户】完成，匹配结果：未匹配上:") /* "对方信息辨识规则,辨识【客户】完成，匹配结果：未匹配上:" */),
        OPPOSITE_TYPE_START_INNER_UNIT("00406",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003DC", "开始执行对方信息辨识规则,进行【内部单位】辨识：") /* "开始执行对方信息辨识规则,进行【内部单位】辨识：" */),
        OPPOSITE_TYPE_START_INNER_RESULT("00407",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003DD", "对方信息辨识规则,辨识【内部单位】完成，匹配结果：设置对方类型为【内部单位】,已匹配:") /* "对方信息辨识规则,辨识【内部单位】完成，匹配结果：设置对方类型为【内部单位】,已匹配:" */),
        OPPOSITE_TYPE_START_INNER_RESULT_NO("00408",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003DE", "对方信息辨识规则,辨识【内部单位】完成，匹配结果：未匹配上:") /* "对方信息辨识规则,辨识【内部单位】完成，匹配结果：未匹配上:" */),
        OPPOSITE_TYPE_START_STAFF_UNIT("00409",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003DF", "开始执行对方信息辨识规则,进行【员工】辨识：") /* "开始执行对方信息辨识规则,进行【员工】辨识：" */),
        OPPOSITE_TYPE_START_STAFF_RESULT("00410",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003E0", "对方信息辨识规则,辨识【员工】完成，匹配结果：设置对方类型为【员工】,已匹配:") /* "对方信息辨识规则,辨识【员工】完成，匹配结果：设置对方类型为【员工】,已匹配:" */),
        OPPOSITE_TYPE_START_STAFF_RESULT_NO("00411",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003E1", "对方信息辨识规则,辨识【员工】完成，匹配结果：未匹配上:") /* "对方信息辨识规则,辨识【员工】完成，匹配结果：未匹配上:" */),
        OPPOSITE_TYPE_START_VENDOR_UNIT("00412",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003E2", "开始执行对方信息辨识规则,进行【供应商】辨识：") /* "开始执行对方信息辨识规则,进行【供应商】辨识：" */),
        OPPOSITE_TYPE_START_VENDOR_RESULT("00413",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003E3", "对方信息辨识规则,辨识【供应商】完成，匹配结果：设置对方类型为【供应商】,已匹配:") /* "对方信息辨识规则,辨识【供应商】完成，匹配结果：设置对方类型为【供应商】,已匹配:" */),
        OPPOSITE_TYPE_START_VENDOR_RESULT_NO("00414",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003E4", "对方信息辨识规则,辨识【供应商】完成，匹配结果：未匹配上:") /* "对方信息辨识规则,辨识【供应商】完成，匹配结果：未匹配上:" */),
        OPPOSITE_TYPE_START_OTHER_UNIT("00415",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003E5", "对方信息辨识规则,都没匹配上，设置对方类型为【其他】") /* "对方信息辨识规则,都没匹配上，设置对方类型为【其他】" */),
        OPPOSITE_TYPE_ONE("00416",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003E6", "授权使用组织未确认，不能执行对方信息辨识") /* "授权使用组织未确认，不能执行对方信息辨识" */),
        OPPOSITE_TYPE_START_CUSTOMER_RESULT_CACHE("00417",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003E7", "对方信息辨识规则,辨识【客户】完成，匹配结果：从缓存中读取数据设置对方类型为【客户】,已匹配:") /* "对方信息辨识规则,辨识【客户】完成，匹配结果：从缓存中读取数据设置对方类型为【客户】,已匹配:" */),
        OPPOSITE_TYPE_START_CUSTOMER_RESULT_NAME("00404",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003E8", "对方信息辨识规则,辨识【客户】完成，通过[名称]匹配结果：设置对方类型为【客户】,已匹配:") /* "对方信息辨识规则,辨识【客户】完成，通过[名称]匹配结果：设置对方类型为【客户】,已匹配:" */),


        OTHER_INFORMATION_NAME("00501",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003E9", "其他信息辨识") /* "其他信息辨识" */),
        OTHER_INFORMATION_START("00502",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003EA", "开始执行其它匹配规则:") /* "开始执行其它匹配规则:" */),
        OTHER_INFORMATION_ONE("00503",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003EB", "执行其它匹配规则完成,匹配结果：已匹配辨识规则：") /* "执行其它匹配规则完成,匹配结果：已匹配辨识规则：" */),
        OTHER_INFORMATION_TWO("00504",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003EC", "执行其它匹配规则完成,匹配结果：未匹配辨识规则：") /* "执行其它匹配规则完成,匹配结果：未匹配辨识规则：" */),



        OUR_INFORMATION_NAME("00601",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003ED", "本方信息匹配") /* "本方信息匹配" */),
        OUR_INFORMATION_START("00602",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003EE", "开始执行本方信息匹配规则辨识:") /* "开始执行本方信息匹配规则辨识:" */),
        OUR_INFORMATION_ONE("00603",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003EF", "本方信息匹配规则辨识,企业银行账户信息的授权使用组织只有一个，设置资金组织") /* "本方信息匹配规则辨识,企业银行账户信息的授权使用组织只有一个，设置资金组织" */),


        PAYMENT_BILL_NAME("00701",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003F0", "收付单据匹配") /* "收付单据匹配" */),
        PAYMENT_BILL_START("00702",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003F1", "开始执行收付单据规则:") /* "开始执行收付单据规则:" */),
        PAYMENT_BILL_ONE("00703",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003F2", "【智能流水收付单匹配】未查询到结算单信息!") /* "【智能流水收付单匹配】未查询到结算单信息!" */),
        PAYMENT_BILL_TWO("00704",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003F3", "收付单据匹配调用结算单接口，构建请求参数") /* "收付单据匹配调用结算单接口，构建请求参数" */),
        PAYMENT_BILL_THREE("00705",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003F4", "【智能流水收付单匹配】调结算接口返回值") /* "【智能流水收付单匹配】调结算接口返回值" */),
        PAYMENT_BILL_FOUR("00706",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003F5", "【智能流水收付单匹配】单据需要手工处理！匹配的规则为：") /* "【智能流水收付单匹配】单据需要手工处理！匹配的规则为：" */),
        PAYMENT_BILL_FIVE("00707",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003F6", "【智能流水收付单匹配】单据自动关联，不需要人工确认！匹配的规则为：") /* "【智能流水收付单匹配】单据自动关联，不需要人工确认！匹配的规则为：" */),
        PAYMENT_BILL_SEX("00703",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003F7", "【智能流水收付单匹配】已匹配到结算单信息:") /* "【智能流水收付单匹配】已匹配到结算单信息:" */),
        PAYMENT_BILL_SIX("00704",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003F8", "【智能流水收付单匹配】发布cmp_bankreconciliation_corrbill类型事件通知结算处理关联关系！") /* "【智能流水收付单匹配】发布cmp_bankreconciliation_corrbill类型事件通知结算处理关联关系！" */),
        PAYMENT_BILL_TEN("00705",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003F9", "【智能流水收付单匹配】财资统一码处理中被移除！") /* "【智能流水收付单匹配】财资统一码处理中被移除！" */),


        PENDING_ACCOUNT_IDENTIFY_NAME("00801",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003FA", "挂账辨识") /* "挂账辨识" */),
        PENDING_ACCOUNT_IDENTIFY_START("00801",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003FB", "开始执行挂账辨识匹配规则：") /* "开始执行挂账辨识匹配规则：" */),
        PENDING_ACCOUNT_IDENTIFY_ONE("00802",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003FC", "【挂账辨识匹配】已执行完成，匹配结果：已经匹配上:") /* "【挂账辨识匹配】已执行完成，匹配结果：已经匹配上:" */),
        PENDING_ACCOUNT_IDENTIFY_TWO("00803",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003FD", "【挂账辨识匹配】已执行完成，匹配结果：未匹配上:") /* "【挂账辨识匹配】已执行完成，匹配结果：未匹配上:" */),


        PUBLISH_OBJ_NAME("00901",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003FE", "发布对象辨识") /* "发布对象辨识" */),
        PUBLISH_OBJ_START("00902",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003FF", "开始执行发布对象辨识规则：") /* "开始执行发布对象辨识规则：" */),
        PUBLISH_OBJ_ONE("00903",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400400", "【发布对象辨识规则辨识】已执行完成，匹配结果：已经匹配上:") /* "【发布对象辨识规则辨识】已执行完成，匹配结果：已经匹配上:" */),
        PUBLISH_OBJ_TWO("00904",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400401", "【发布对象辨识规则辨识】已执行完成，匹配结果：未匹配上:") /* "【发布对象辨识规则辨识】已执行完成，匹配结果：未匹配上:" */),


        GENERATE_BILL_TYPE_PROCESS_NAME("01001",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003CF", "生单类型辨识") /* "生单类型辨识" */),
        GENERATE_BILL_TYPE_PROCESS_IDENTIFY_NAME("01002",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400402", "挂账辨识规则生单") /* "挂账辨识规则生单" */),
        GENERATE_BILL_TYPE_PROCESS_ONE("01003",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400403", "执行生单业务逻辑，调用相关生单接口：businessGenerateFundNewService.bankreconciliationGenerateDoc") /* "执行生单业务逻辑，调用相关生单接口：businessGenerateFundNewService.bankreconciliationGenerateDoc" */),
        GENERATE_BILL_TYPE_PROCESS_TWO("01004",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400404", "执行生单业务逻辑异常:") /* "执行生单业务逻辑异常:" */),
        GENERATE_BILL_TYPE_PROCESS_IDENTIFY_ONE("01005",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400405", "执行自动确认生单业务逻辑") /* "执行自动确认生单业务逻辑" */),
        GENERATE_BILL_TYPE_PROCESS_IDENTIFY_TWO("01006",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400406", "执行自动确认生单业务逻辑异常:") /* "执行自动确认生单业务逻辑异常:" */),
        GENERATE_BILL_TYPE_PROCESS_IDENTIFY_THREE("01007",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400407", "此生单会调用消息队列进行异步消费，辨识流程提示成功后，请耐心等待状态的异步消息消费，回刷银行流水状态") /* "此生单会调用消息队列进行异步消费，辨识流程提示成功后，请耐心等待状态的异步消息消费，回刷银行流水状态" */),
        GENERATE_BILL_TYPE_PROCESS_IDENTIFY_FOUR("01008",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400408", "校验对账单状态失败，不能进行生单操作！") /* "校验对账单状态失败，不能进行生单操作！" */),
        GENERATE_BILL_TYPE_PROCESS_IDENTIFY_FIVE("01009",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400409", "是疑似重复数据，不进行后续操作！") /* "是疑似重复数据，不进行后续操作！" */),



        BANK_ERROR_NAME("02001",com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540040A", "程序异常信息：") /* "程序异常信息：" */),

        BANK_SERIALIZATION_ERROR_NAME("02002", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21CA10C004880008", "对象序列化异常") /* "对象序列化异常" */),
        BANK_SERIALIZATION_ERROR_MESSAGE("02002", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21CA10C004880008", "对象序列化异常") /* "对象序列化异常" */),
        ;




        private String status;
        private String desc;
        RuleLogProcess(String status,String desc){
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
}
