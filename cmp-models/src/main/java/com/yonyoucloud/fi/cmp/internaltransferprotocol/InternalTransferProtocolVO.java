package com.yonyoucloud.fi.cmp.internaltransferprotocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <h1>银行对账单调用内转协议生单时入参</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023-09-11 14:40
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class InternalTransferProtocolVO implements Serializable {
    private static final long serialVersionUID = 2531129628687055183L;
    // *资金组织
    private String accentity;
    // *项目
    private String project;
    // 合同编码
    private String contractNo;
    // 合同名称
    private String contractName;
    // *待拆分总额
    private BigDecimal splitAmount;
    // *币种
    private String currency;
    // 转出方银行账户ID
    private String enterpriseBankAccount;
    // 开户银行大类
    private Short acctOpenType;
    // *来源单据ID
    private String srcBillId;
    // 来源单据编码Code
    private String srcBillCode;
    // 来源单据类型
    private String srcBillType;
    // 内转协议id
    private String protocolId;
    //有效内转协议
    private List<Map<String, Object>> validInternalTransferBills;
}
