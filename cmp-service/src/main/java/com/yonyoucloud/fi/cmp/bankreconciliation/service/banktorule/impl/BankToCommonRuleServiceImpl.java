package com.yonyoucloud.fi.cmp.bankreconciliation.service.banktorule.impl;

import com.yonyou.ucf.basedoc.model.BankdotVO;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.service.banktorule.BankToCommonRuleService;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.billclaim.BillClaimItem;
import com.yonyoucloud.fi.cmp.cmpentity.CaObject;
import com.yonyoucloud.fi.cmp.cmpentity.OppositeType;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.MerchantConstant;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankrecilication.CtmcmpReWriteBusRpcService;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.VendorQueryService;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialQryDTO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author hanll
 * @Date 2024/7/4-09:59
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BankToCommonRuleServiceImpl implements BankToCommonRuleService {

    private final String bankname = "bankname";
    private final String isdefault = "isdefault";
    private final String staff_id = "staff_id";
    private final String defaultbank = "defaultbank";
    private final String vendor = "vendor";
    private final String dr = "dr";
    private final String oppositeaccountname = "oppositeaccountname";
    private final String oppositeaccountno = "oppositeaccountno";
    private final String caobject = "caobject";
    private final String oppositeobjectid = "oppositeobjectid";
    private final String oppositeaccountid = "oppositeaccountid";
    private final String stopstatus = "stopstatus";
    private final BaseRefRpcService baseRefRpcService;
    private final VendorQueryService vendorQueryService;


    /**
     * 填写对方信息
     *
     * @param mapSub 单据转换规则实体
     * @throws Exception
     */
    @Override
    public void fillOppositeInfo(Map<String, Object> mapSub, Map<String, Object> paramMap, Map<Long, BankReconciliation> bankReconciliationMap, Map<String, BillClaimItem> billClaimItemMap) throws Exception {
        // 付款银行账号
        String toAcctNo = null == mapSub.get(oppositeaccountno) ? null : mapSub.get(oppositeaccountno).toString();
        BankReconciliation bankReconciliation;
        if (BillClaim.ENTITY_NAME.equals(paramMap.get("sourceFullName").toString())) {
            // 到账认领单
            Object srcbillId = mapSub.get("srcbillid");
            BillClaimItem item = billClaimItemMap.get(srcbillId.toString());
            // 认领单转换成银行对账单
            bankReconciliation = convertClaimItemToBankRc(item);
        } else {
            // 银行对账单
            Long bankReconciliationId = Long.parseLong(mapSub.get("bankReconciliationId").toString());
            // 查询银行转账单数据
            bankReconciliation = bankReconciliationMap.get(bankReconciliationId);
        }
        // 对方类型
        Short oppositetype = bankReconciliation.getOppositetype();
        // 其他 对方类型为空默认为其他
        if (oppositetype == null || oppositetype == OppositeType.Other.getValue()) {
            mapSub.put(caobject, CaObject.Other.getValue());
            mapSub.put(oppositeobjectid, bankReconciliation.getOppositeobjectid());
            mapSub.put(ICmpConstant.OPPOSITEOBJECTNAME, bankReconciliation.getTo_acct_name());
        } else if (oppositetype == OppositeType.Supplier.getValue()) {
            // 供应商
            mapSub.put(caobject, CaObject.Supplier.getValue());
            // 对方银行账户Id
            mapSub.put(oppositeaccountid, bankReconciliation.getTo_acct());
            // 对方银行账号
            mapSub.put(oppositeaccountno, toAcctNo);
            // 对方银行账户名称
            mapSub.put(oppositeaccountname, bankReconciliation.getTo_acct_name());
            // 对方银行类型
            mapSub.put("oppositebankType", null);
            // 对方单位Id
            mapSub.put(oppositeobjectid, bankReconciliation.getOppositeobjectid());
            // 对方单位名称
            mapSub.put(ICmpConstant.OPPOSITEOBJECTNAME, bankReconciliation.getOppositeobjectname());
        } else if (oppositetype == OppositeType.Customer.getValue()) {
            // 客户
            mapSub.put(caobject, CaObject.Customer.getValue());
            // 对方银行类型
            mapSub.put("oppositebankType", null);
            // 对方银行账户id
            mapSub.put(oppositeaccountid, bankReconciliation.getTo_acct());
            // 付款方账户名称
            mapSub.put(oppositeaccountname, bankReconciliation.getTo_acct_name());
            // 对方银行账号
            mapSub.put(oppositeaccountno, toAcctNo);
            // 对方单位id
            mapSub.put(oppositeobjectid, bankReconciliation.getOppositeobjectid());
            // 对方单位名称
            mapSub.put(ICmpConstant.OPPOSITEOBJECTNAME, bankReconciliation.getOppositeobjectname());
        } else if (oppositetype == OppositeType.Employee.getValue()) {
            // 员工
            mapSub.put(caobject, CaObject.Employee.getValue());
            // 对方银行类型
            mapSub.put("oppositebankType", null);
            // 对方银行账户Id
            mapSub.put(oppositeaccountid, bankReconciliation.getTo_acct());
            // 对方银行账号
            mapSub.put(oppositeaccountno, toAcctNo);
            // 对方银行账户名称
            mapSub.put(oppositeaccountname, bankReconciliation.getTo_acct_name());
            // 对方单位Id
            mapSub.put(oppositeobjectid, bankReconciliation.getOppositeobjectid());
            // 对方单位名称
            mapSub.put(ICmpConstant.OPPOSITEOBJECTNAME, bankReconciliation.getOppositeobjectname());
        } else if (oppositetype == OppositeType.InnerOrg.getValue()) {
            // 内部单位
            mapSub.put(caobject, CaObject.InnerUnit.getValue());
            // 对方银行账户Id
            mapSub.put(oppositeaccountid, bankReconciliation.getTo_acct());
        }
        // 财资统一对账码赋值
        if (bankReconciliation.getSmartcheckno() != null) {
            mapSub.put("smartcheckno", bankReconciliation.getSmartcheckno());
        }
    }


    /**
     * 认领单转换成银行对账单
     *
     * @param item 认领单明细
     * @return 银行对账单
     */
    private BankReconciliation convertClaimItemToBankRc(BillClaimItem item) {
        BankReconciliation bankReconciliation = new BankReconciliation();
        // 对方类型
        bankReconciliation.setOppositetype(item.getOppositetype());
        // 对方银行账户Id
        bankReconciliation.setTo_acct(item.getToAcct());
        // 对方银行账号
        bankReconciliation.setTo_acct_no(item.getTo_acct_no());
        // 对方银行账户名称
        bankReconciliation.setTo_acct_name(item.getTo_acct_name());
        // 对方单位Id
        bankReconciliation.setOppositeobjectid(item.getOppositeobjectid());
        // 对方单位名称
        bankReconciliation.setOppositeobjectname(item.getOppositeobjectname());
        // 开户行
        bankReconciliation.setTo_acct_bank(item.getTo_acct_bank());
        return bankReconciliation;
    }

    private void assignOtherBankInfo(Map<String, Object> map_sub) throws Exception {
        boolean caobjectFlag = ValueUtils.isNotEmptyObj(map_sub.get(caobject)) && Short.valueOf(map_sub.get(caobject).toString()) != CaObject.Other.getValue();
        if (caobjectFlag) {
            Short caObject = Short.valueOf(map_sub.get(caobject).toString());
            if (CaObject.Supplier.getValue() == caObject && ValueUtils.isNotEmptyObj(map_sub.get("oppositeaccountid"))) {
                Map<String, Object> condition = new HashMap<>();
                condition.put("id", map_sub.get("oppositeaccountid"));
                List<VendorBankVO> bankAccounts = vendorQueryService.getVendorBanksByCondition(condition);
                if (bankAccounts.size() > 0) {
                    String openbankid = bankAccounts.get(0).getOpenaccountbank();
                    if (ValueUtils.isNotEmptyObj(openbankid)) {
                        BankdotVO depositBank = baseRefRpcService.queryBankdotVOByBanddotId(openbankid);
                        if (depositBank != null) {
                            map_sub.put("oppositebankaddr", depositBank.getName()); // 供应商账户银行网点
                            map_sub.put("oppositebankaddrid", depositBank.getId()); // 供应商账户银行网点ID
                            map_sub.put("oppositebanklineno", depositBank.getLinenumber()); // 开户行联行号
                            map_sub.put("oppositebankTypeId", depositBank.getBank()); // 银行类别id
                            map_sub.put("oppositebankType", depositBank.getBankName()); // 银行类别name
                            map_sub.put("receivePartySwift", depositBank.getSwiftCode()); // 收款方swift码
                            map_sub.put("receivebankaddr_address", depositBank.getAddress());//收款方开户行地址
                        }
                    }
                }
            } else if (CaObject.Customer.getValue() == caObject && ValueUtils.isNotEmptyObj(map_sub.get("oppositeaccountno"))) {
                AgentFinancialQryDTO agentFinancialQryDTO = new AgentFinancialQryDTO();
                if (map_sub.get("oppositeobjectid") != null) {
                    agentFinancialQryDTO.setMerchantId(Long.valueOf(map_sub.get("oppositeobjectid").toString()));
                }
                agentFinancialQryDTO.setBankAccount(map_sub.get("oppositeaccountno").toString());
                List<AgentFinancialDTO> bankAccounts = QueryBaseDocUtils.queryCustomerBankAccountByCondition(agentFinancialQryDTO);
                if (bankAccounts.size() > 0) {
                    // 查询开户行
                    if (ValueUtils.isNotEmptyObj(bankAccounts.get(0).getOpenBank())) {
                        BankdotVO depositBank = baseRefRpcService.queryBankdotVOByBanddotId(bankAccounts.get(0).getOpenBank());
                        if (depositBank != null) {
                            map_sub.put("oppositebankaddr", depositBank.getName()); // 客户账户银行网点
                            map_sub.put("oppositebankaddrid", depositBank.getId()); // 客户账户银行网点ID
                            map_sub.put("oppositebanklineno", depositBank.getLinenumber()); // 开户行联行号
                            map_sub.put("oppositebankTypeId", depositBank.getBank()); // 银行类别id
                            map_sub.put("oppositebankType", depositBank.getBankName()); // 银行类别name
                            map_sub.put("receivePartySwift", depositBank.getSwiftCode()); // 收款方swift码
                            map_sub.put("receivebankaddr_address", depositBank.getAddress());//收款方开户行地址
                        }
                    }
                }
            } else if (CaObject.Employee.getValue() == caObject && ValueUtils.isNotEmptyObj(map_sub.get("oppositeaccountid"))) {
                Map<String, Object> condition = new HashMap<>();
                condition.put("id", map_sub.get("oppositeaccountid"));
                List<Map<String, Object>> bankAccounts = QueryBaseDocUtils.queryStaffBankAccountByCondition(condition);
                if (bankAccounts.size() > 0) {
                    if (ValueUtils.isNotEmptyObj(bankAccounts.get(0).get("bankname"))) {
                        BankdotVO depositBank = baseRefRpcService.queryBankdotVOByBanddotId(bankAccounts.get(0).get("bankname").toString());
                        if (depositBank != null) {
                            map_sub.put("oppositebankaddr", depositBank.getName()); // 员工账户银行网点
                            map_sub.put("oppositebankaddrid", depositBank.getId()); // 员工账户银行网点ID
                            map_sub.put("oppositebanklineno", depositBank.getLinenumber()); // 开户行联行号
                            map_sub.put("oppositebankType", depositBank.getBankName()); // 银行类别name
                            map_sub.put("receivePartySwift", depositBank.getSwiftCode()); // 收款方swift码
                            map_sub.put("oppositebankTypeId", depositBank.getBank()); // 银行类别id
                            map_sub.put("receivebankaddr_address", depositBank.getAddress());//收款方开户行地址
                        }
                    }
                }
            } else if (CaObject.InnerUnit.getValue() == caObject && ValueUtils.isNotEmptyObj(map_sub.get("oppositeaccountno"))) {
                EnterpriseParams enterpriseParams = new EnterpriseParams();
                enterpriseParams.setAccount(map_sub.get("oppositeaccountno").toString());
                List<EnterpriseBankAcctVO> bankAccounts = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
                if (!bankAccounts.isEmpty()) {
                    EnterpriseBankAcctVO enterpriseBankAcctVO = bankAccounts.get(0);
                    map_sub.put("oppositebankaddrid", enterpriseBankAcctVO.getBankNumber());
                    map_sub.put("oppositebankaddr", enterpriseBankAcctVO.getBankNumberName());
                    map_sub.put("oppositebanklineno", enterpriseBankAcctVO.getLineNumber());
                    map_sub.put("oppositebankType", enterpriseBankAcctVO.getBankName());
                }
            }
        }
    }

}
