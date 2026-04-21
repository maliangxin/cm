package com.yonyoucloud.fi.cmp.transferaccount.rule;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount_b;
import com.yonyoucloud.fi.cmp.batchtransferaccount.utils.BatchTransferAccountUtil;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.enums.YesOrNoEnum;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.TransTypeQueryService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryOrderby;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 转账单，参照前规则 - 10
 */
@Component("taReferRule")
public class TaReferRule extends AbstractCommonRule {
    @Autowired
    TransTypeQueryService transTypeQueryService;
    @Autowired
    AutoConfigService autoConfigService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        BizObject bizObject = null;
        String billnum = billDataDto.getBillnum();
        List<BizObject> bills = getBills(billContext, map);
        if (bills.size() > 0) {
            bizObject = bills.get(0);
        }
        if (IBillNumConstant.CMP_BATCHTRANSFERACCOUNT.equals(billnum) || IBillNumConstant.CMP_BATCHTRANSFERACCOUNTLIST.equals(billnum)) {
            //批量同名账户划转过滤逻辑
            filterOfBatchTransferAccount(bizObject, billDataDto);
        } else if (IBillNumConstant.TRANSFERACCOUNT.equals(billnum) || IBillNumConstant.TRANSFERACCOUNTLIST.equals(billnum)) {
            //同名账户划转过滤逻辑
            filterOfTransferAccount(bizObject, billDataDto);
        }
        return new RuleExecuteResult();
    }

    /**
     * 批量转账单的参照过滤逻辑
     *
     * @param bizObject
     * @param billDataDto
     * @throws Exception
     */
    void filterOfBatchTransferAccount(BizObject bizObject, BillDataDto billDataDto) throws Exception {
        BatchTransferAccount batchTransferAccount = (BatchTransferAccount) bizObject;
        if ("cmp_checkRef".equals(billDataDto.getrefCode())) {
            BatchTransferAccount_b batchTransferAccount_b = batchTransferAccount.BatchTransferAccount_b().get(0);
            String accentity = batchTransferAccount.getAccentity();//会计主体
            // 付款是否关联
            boolean associationStatusPay = batchTransferAccount_b.getAssociationStatusPay() != null
                    && batchTransferAccount_b.getAssociationStatusPay() == YesOrNoEnum.YES.getValue();
            String payBankAccount = batchTransferAccount_b.getPayBankAccountId();//付款银行账户
            FilterVO conditon = new FilterVO();
            if (billDataDto.getCondition() == null) {
                //支票类型
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("checkBillType", ICmpConstant.QUERY_IN, new Integer[]{0, 1, 3, 4, 5}));
                //币种
                String currency = bizObject.get("currency");
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", ICmpConstant.QUERY_EQ, currency));
                //支票状态
                // 如果开启领用则判断支票状态是否为已领用，如果未开启领用则判断支票状态是否为已入库
                if (autoConfigService.getCheckStockCanUse()) {
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("checkBillStatus", ICmpConstant.QUERY_EQ, 13));
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("custNo", ICmpConstant.QUERY_EQ, accentity));
                } else {
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("checkBillStatus", ICmpConstant.QUERY_EQ, 1));
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("accentity", ICmpConstant.QUERY_EQ, accentity));
                }
                //支票方向
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("checkBillDir", ICmpConstant.QUERY_EQ, 2));
                //支票是否被占用
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("occupy", ICmpConstant.QUERY_EQ, 0));

                if (associationStatusPay) {
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("drawerAcct", ICmpConstant.QUERY_EQ, payBankAccount));
                }
                billDataDto.setCondition(conditon);
            } else {
                //支票类型
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("checkBillType", ICmpConstant.QUERY_IN, new Integer[]{0, 1, 3, 4, 5}));
                //币种
                String currency = bizObject.get("currency");
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", ICmpConstant.QUERY_EQ, currency));
                //支票状态
                // 如果开启领用则判断支票状态是否为已领用，如果未开启领用则判断支票状态是否为已入库
                if (autoConfigService.getCheckStockCanUse()) {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("checkBillStatus", ICmpConstant.QUERY_EQ, 13));
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("custNo", ICmpConstant.QUERY_EQ, accentity));
                } else {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("checkBillStatus", ICmpConstant.QUERY_EQ, 1));
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("accentity", ICmpConstant.QUERY_EQ, accentity));
                }
                //支票方向
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("checkBillDir", ICmpConstant.QUERY_EQ, 2));
                //支票是否被占用
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("occupy", ICmpConstant.QUERY_EQ, 0));

                if (associationStatusPay) {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("drawerAcct", ICmpConstant.QUERY_EQ, payBankAccount));
                }
            }

        }
    }

    /**
     * 转账单的参照过滤逻辑
     *
     * @param bizObject
     * @param billDataDto
     * @throws Exception
     */
    void filterOfTransferAccount(BizObject bizObject, BillDataDto billDataDto) throws Exception {
        if ("cmp_checkRef".equals(billDataDto.getrefCode())) {
            filterCheckRef(bizObject, billDataDto);
        }
        if ("transtype.bd_billtyperef".equals(billDataDto.getrefCode())) {
            FilterVO conditon = new FilterVO();
            if (billDataDto.getCondition() == null) {
                billDataDto.setCondition(conditon);
            }
            filterBdBillType(bizObject, billDataDto);
            // 银行账户参照
        } else if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(billDataDto.getrefCode()) && bizObject != null) {
            FilterVO conditon = new FilterVO();
            if (billDataDto.getCondition() == null) {
                if (bizObject.get("currency") != null) {
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO(ICmpConstant.CURRENCY_REf, ICmpConstant.QUERY_EQ, bizObject.get("currency")));
                }
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO(ICmpConstant.CURRENCY_ENABLE_REf, ICmpConstant.QUERY_EQ, "1"));
                if ("payBankAccount_name".equals(billDataDto.getKey()) && bizObject.get("recBankAccount") != null) {
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_NEQ, bizObject.get("recBankAccount")));
                } else if ("recBankAccount_name".equals(billDataDto.getKey()) && bizObject.get("payBankAccount") != null) {
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_NEQ, bizObject.get("payBankAccount")));
                }
                billDataDto.setCondition(conditon);
            } else {
                if (bizObject.get("currency") != null) {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO(ICmpConstant.CURRENCY_REf, ICmpConstant.QUERY_EQ, bizObject.get("currency")));
                }
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO(ICmpConstant.CURRENCY_ENABLE_REf, ICmpConstant.QUERY_EQ, "1"));
                if ("payBankAccount_name".equals(billDataDto.getKey()) && bizObject.get("recBankAccount") != null) {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_NEQ, bizObject.get("recBankAccount")));
                } else if ("recBankAccount_name".equals(billDataDto.getKey()) && bizObject.get("payBankAccount") != null) {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_NEQ, bizObject.get("payBankAccount")));
                }
            }
        } else if ("productcenter.aa_settlemethodref".equals(billDataDto.getrefCode())) {
            // 过滤结算方式
            filterSettlemethod(bizObject, billDataDto);
        }  else if ("bd_enterprisecashacctref".equals(billDataDto.getrefCode()) && bizObject != null) {
            FilterVO conditon = new FilterVO();
            if (billDataDto.getCondition() == null) {
                if (bizObject.get("currency") != null) {
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", ICmpConstant.QUERY_EQ, bizObject.get("currency")));
                }
                if ("payCashAccount_name".equals(billDataDto.getKey()) && bizObject.get("recCashAccount") != null) {
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_NEQ, bizObject.get("recCashAccount")));
                } else if ("recCashAccount_name".equals(billDataDto.getKey()) && bizObject.get("payCashAccount") != null) {
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_NEQ, bizObject.get("payCashAccount")));
                }
                billDataDto.setCondition(conditon);
            } else {
                if (bizObject.get("currency") != null) {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", ICmpConstant.QUERY_EQ, bizObject.get("currency")));
                }
                if ("payCashAccount_name".equals(billDataDto.getKey()) && bizObject.get("recCashAccount") != null) {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_NEQ, bizObject.get("recCashAccount")));
                } else if ("recCashAccount_name".equals(billDataDto.getKey()) && bizObject.get("payCashAccount") != null) {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_NEQ, bizObject.get("payCashAccount")));
                }
            }

        }
        //银行流水业务处理生成转账单时，关联对方流水时，没有根据银行账户过滤  对账单
        if ("cmp_bankreconciliationlistRef".equals(billDataDto.getrefCode()) && bizObject != null && null != billDataDto.getKey() && billDataDto.getKey().equals("paybankbill_bank_seq_no")) {
            //recBankAccount
            FilterVO conditon = new FilterVO();
            if (billDataDto.getCondition() == null) {
                if (bizObject.get("recBankAccount") != null) {
                    //根据银行账号id获取银行账号
                    EnterpriseBankAcctVO enterpriseBankAcctVO = EnterpriseBankQueryService.findById(bizObject.getJavaObject("recBankAccount", String.class));
                    if (ObjectUtils.isNotEmpty(enterpriseBankAcctVO)) {
                        String bankAccount = enterpriseBankAcctVO.getAccount();
                        conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("to_acct_no", ICmpConstant.QUERY_EQ, bankAccount));
                    }
                }
                //本方
                if (bizObject.get("payBankAccount") != null) {
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("bankaccount", ICmpConstant.QUERY_EQ, bizObject.get("payBankAccount")));
                }
                billDataDto.setCondition(conditon);
            } else {
                if (bizObject.get("recBankAccount") != null) {
                    //根据银行账号id获取银行账号
                    EnterpriseBankAcctVO enterpriseBankAcctVO = EnterpriseBankQueryService.findById(bizObject.getJavaObject("recBankAccount", String.class));
                    if (ObjectUtils.isNotEmpty(enterpriseBankAcctVO)) {
                        String bankAccount = enterpriseBankAcctVO.getAccount();
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("to_acct_no", ICmpConstant.QUERY_EQ, bankAccount));
                    }
                }
                //本方
                if (bizObject.get("payBankAccount") != null) {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("bankaccount", ICmpConstant.QUERY_EQ, bizObject.get("payBankAccount")));
                }
            }
        }

        //银行流水业务处理生成转账单时，关联对方流水时，没有根据银行账户过滤   对账单
        if ("cmp_bankreconciliationlistRef".equals(billDataDto.getrefCode()) && bizObject != null && null != billDataDto.getKey() && billDataDto.getKey().equals("collectbankbill_bank_seq_no")) {
            //recBankAccount
            FilterVO conditon = new FilterVO();
            if (billDataDto.getCondition() == null) {
                //对方
                if (bizObject.get("payBankAccount") != null) {
                    //根据银行账号id获取银行账号
                    EnterpriseBankAcctVO enterpriseBankAcctVO = EnterpriseBankQueryService.findById(bizObject.getJavaObject("payBankAccount", String.class));
                    if (ObjectUtils.isNotEmpty(enterpriseBankAcctVO)) {
                        String bankAccount = enterpriseBankAcctVO.getAccount();
                        conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("to_acct_no", ICmpConstant.QUERY_EQ, bankAccount));
                    }
                }
                //本方
                if (bizObject.get("recBankAccount") != null) {
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("bankaccount", ICmpConstant.QUERY_EQ, bizObject.get("recBankAccount")));
                }
                billDataDto.setCondition(conditon);
            } else {
                if (bizObject.get("payBankAccount") != null) {
                    //根据银行账号id获取银行账号
                    EnterpriseBankAcctVO enterpriseBankAcctVO = EnterpriseBankQueryService.findById(bizObject.getJavaObject("payBankAccount",String.class));
                    if (ObjectUtils.isNotEmpty(enterpriseBankAcctVO)) {
                        String bankAccount = enterpriseBankAcctVO.getAccount();
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("to_acct_no", ICmpConstant.QUERY_EQ, bankAccount));
                    }
                }
                //本方
                if (bizObject.get("recBankAccount") != null) {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("bankaccount", ICmpConstant.QUERY_EQ, bizObject.get("recBankAccount")));
                }
            }
        }

        //银行流水业务处理生成转账单时，关联对方流水时，没有根据银行账户过滤   认领单
        if ("cmp_mybillclaimlistRef".equals(billDataDto.getrefCode()) && bizObject != null && null != billDataDto.getKey() && billDataDto.getKey().equals("paybillclaim_code")) {
            //recBankAccount
            FilterVO conditon = new FilterVO();
            if (billDataDto.getCondition() == null) {
                if (bizObject.get("recBankAccount") != null) {
                    //根据银行账号id获取银行账号
                    EnterpriseBankAcctVO enterpriseBankAcctVO = EnterpriseBankQueryService.findById(bizObject.getJavaObject("recBankAccount",String.class));
                    if (ObjectUtils.isNotEmpty(enterpriseBankAcctVO)) {
                        String bankAccount = enterpriseBankAcctVO.getAccount();
                        conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("toaccountno", ICmpConstant.QUERY_EQ, bankAccount));
                    }
                }
                //本方
                if (bizObject.get("payBankAccount") != null) {
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("bankaccount", ICmpConstant.QUERY_EQ, bizObject.get("payBankAccount")));
                }
                //要过滤认领状态 应该只能参照到已认领的认领单
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("recheckstatus", ICmpConstant.QUERY_EQ, 1));
                billDataDto.setCondition(conditon);
            } else {
                if (bizObject.get("recBankAccount") != null) {
                    //根据银行账号id获取银行账号
                    EnterpriseBankAcctVO enterpriseBankAcctVO = EnterpriseBankQueryService.findById(bizObject.getJavaObject("recBankAccount",String.class));
                    if (ObjectUtils.isNotEmpty(enterpriseBankAcctVO)) {
                        String bankAccount = enterpriseBankAcctVO.getAccount();
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("toaccountno", ICmpConstant.QUERY_EQ, bankAccount));
                    }
                }
                //本方
                if (bizObject.get("payBankAccount") != null) {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("bankaccount", ICmpConstant.QUERY_EQ, bizObject.get("payBankAccount")));
                }
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("recheckstatus", ICmpConstant.QUERY_EQ, 1));
            }
        }

        //银行流水业务处理生成转账单时，关联对方流水时，没有根据银行账户过滤  认领单
        if ("cmp_mybillclaimlistRef".equals(billDataDto.getrefCode()) && bizObject != null && null != billDataDto.getKey() && billDataDto.getKey().equals("collectbillclaim_code")) {
            //recBankAccount
            FilterVO conditon = new FilterVO();
            if (billDataDto.getCondition() == null) {
                //对方
                if (bizObject.get("payBankAccount") != null) {
                    //根据银行账号id获取银行账号
                    EnterpriseBankAcctVO enterpriseBankAcctVO = EnterpriseBankQueryService.findById(bizObject.getJavaObject("payBankAccount",String.class));
                    if (ObjectUtils.isNotEmpty(enterpriseBankAcctVO)) {
                        String bankAccount = enterpriseBankAcctVO.getAccount();
                        conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("toaccountno", ICmpConstant.QUERY_EQ, bankAccount));
                    }
                }
                //本方
                if (bizObject.get("recBankAccount") != null) {
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("bankaccount", ICmpConstant.QUERY_EQ, bizObject.get("recBankAccount")));
                }
                //要过滤认领状态 应该只能参照到已认领的认领单
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("recheckstatus", ICmpConstant.QUERY_EQ, 1));
                billDataDto.setCondition(conditon);
            } else {
                if (bizObject.get("payBankAccount") != null) {
                    //根据银行账号id获取银行账号
                    EnterpriseBankAcctVO enterpriseBankAcctVO = EnterpriseBankQueryService.findById(bizObject.getJavaObject("payBankAccount",String.class));
                    if (ObjectUtils.isNotEmpty(enterpriseBankAcctVO)) {
                        String bankAccount = enterpriseBankAcctVO.getAccount();
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("toaccountno", ICmpConstant.QUERY_EQ, bankAccount));
                    }
                }
                //本方
                if (bizObject.get("recBankAccount") != null) {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("bankaccount", ICmpConstant.QUERY_EQ, bizObject.get("recBankAccount")));
                }
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("recheckstatus", ICmpConstant.QUERY_EQ, 1));
            }
        }


        // 虚拟账户按币种和会计主体过滤
        // 虚拟账户按币种和会计主体过滤
        if ("ucfbasedoc.bd_enterprisebankacct".equals(billDataDto.getrefCode()) && null != billDataDto.getKey() && billDataDto.getKey().equals("collVirtualAccount_name")) {
            if (bizObject.get("payBankAccount_name") != null) {
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_NEQ, bizObject.get("payBankAccount")));
            }
            filterVirtualAccount(bizObject, billDataDto);
        }
        if ("ucfbasedoc.bd_enterprisebankacct".equals(billDataDto.getrefCode()) && null != billDataDto.getKey() && billDataDto.getKey().equals("payVirtualAccount_name")) {
            if (bizObject.get("recBankAccount_name") != null) {
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", ICmpConstant.QUERY_NEQ, bizObject.get("recBankAccount")));
            }
            filterVirtualAccount(bizObject, billDataDto);
        }

        // 企业现金账户过滤
        BatchTransferAccountUtil.filterCashAccount(billDataDto, bizObject);
    }


    /**
     * 过滤交易类型
     * @param bizObject
     * @param billDataDto
     */
    private void filterBdBillType(BizObject bizObject, BillDataDto billDataDto) {
        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("billtype_id", ICmpConstant.QUERY_EQ, "FICA4"));
        if (bizObject == null) {
            return;
        }

        String transferType = null;
        String tradeTypeCode = bizObject.get("tradetype_code");
        String type = bizObject.get("type");
        if ("bt".equals(type) || "BT".equals(tradeTypeCode)) {
            transferType = "yhzz";
        } else if ("sc".equals(type) || "SC".equals(tradeTypeCode)) {
            transferType = "jcxj";
        } else if ("ec".equals(type) || "EC".equals(tradeTypeCode)) {
            transferType = "tqxj";
        } else if ("ct".equals(type) || "CT".equals(tradeTypeCode)) {
            transferType = "xjhz";
        } else if ("tpt".equals(type) || "TPT".equals(tradeTypeCode)) {
            transferType = "dsfzz";
        } else if ("wcz".equals(type) || "WCZ".equals(tradeTypeCode)) {
            transferType = "sbqbcz";
        } else if ("wtx".equals(type) || "WTX".equals(tradeTypeCode)) {
            transferType = "sbqbtx";
        } else if ("whz".equals(type) || "WHZ".equals(tradeTypeCode)) {
            transferType = "sbqbhz";
        }
        if (transferType != null) {
            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("extend_attrs_json", ICmpConstant.QUERY_LIKE, transferType));
        }

    }


    /**
     * 虚拟账户过滤
     *
     * @param bizObject
     * @param billDataDto
     * @throws Exception
     */
    private void filterVirtualAccount(BizObject bizObject, BillDataDto billDataDto) throws Exception {
        String accentity = bizObject.get("accentity").toString();
        if (MapUtils.getString(bizObject, "currency") == null) {
            throw new CtmException(InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180014", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540055B", "请先选择币种") /* "请先选择币种" */) /* "请先选择币种" */);
        }
        String currency = bizObject.get("currency").toString();
        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("orgid", ICmpConstant.QUERY_EQ, accentity));
        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO(ICmpConstant.CURRENCY_REf, ICmpConstant.QUERY_EQ, currency));
        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("enable", ICmpConstant.QUERY_EQ, 1));
        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", ICmpConstant.QUERY_EQ, 3));
    }


    /**
     * 过滤支票
     *
     * @param bizObject
     * @param billDataDto
     */
    private void filterCheckRef(BizObject bizObject, BillDataDto billDataDto) throws Exception {
        String accentity = (String) bizObject.get("accentity");//会计主体
        Boolean associationStatusPay = bizObject.get("associationStatusPay");//付款是否关联
        String payBankAccount = (String) bizObject.get("payBankAccount");//付款银行账户
        FilterVO conditon = new FilterVO();
        if (billDataDto.getCondition() == null) {
            // conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("accentity", ICmpConstant.QUERY_EQ, accentity));
            //支票类型
            conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("checkBillType", ICmpConstant.QUERY_IN, new Integer[]{0, 1, 3, 4, 5}));
            //币种
            String currency = bizObject.get("currency");
            conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", ICmpConstant.QUERY_EQ, currency));
            //支票状态
            // 如果开启领用则判断支票状态是否为已领用，如果未开启领用则判断支票状态是否为已入库
            if (autoConfigService.getCheckStockCanUse()) {
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("checkBillStatus", ICmpConstant.QUERY_EQ, 13));
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("custNo", ICmpConstant.QUERY_EQ, accentity));
            } else {
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("checkBillStatus", ICmpConstant.QUERY_EQ, 1));
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("accentity", ICmpConstant.QUERY_EQ, accentity));
            }
            //支票方向
            conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("checkBillDir", ICmpConstant.QUERY_EQ, 2));
            //支票是否被占用
            conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("occupy", ICmpConstant.QUERY_EQ, 0));
            if (null != associationStatusPay) {
                if (associationStatusPay == true) {
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("drawerAcct", ICmpConstant.QUERY_EQ, payBankAccount));
                }
            }
            billDataDto.setCondition(conditon);
        } else {
            // billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("accentity", ICmpConstant.QUERY_EQ, accentity));
            //支票类型
            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("checkBillType", ICmpConstant.QUERY_IN, new Integer[]{0, 1, 3, 4, 5}));
            //币种
            String currency = bizObject.get("currency");
            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", ICmpConstant.QUERY_EQ, currency));
            //支票状态
            // 如果开启领用则判断支票状态是否为已领用，如果未开启领用则判断支票状态是否为已入库
            if (autoConfigService.getCheckStockCanUse()) {
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("checkBillStatus", ICmpConstant.QUERY_EQ, 13));
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("custNo", ICmpConstant.QUERY_EQ, accentity));
            } else {
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("checkBillStatus", ICmpConstant.QUERY_EQ, 1));
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("accentity", ICmpConstant.QUERY_EQ, accentity));
            }
            //支票方向
            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("checkBillDir", ICmpConstant.QUERY_EQ, 2));
            //支票是否被占用
            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("occupy", ICmpConstant.QUERY_EQ, 0));
            if (null != associationStatusPay) {
                if (associationStatusPay == true) {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("drawerAcct", ICmpConstant.QUERY_EQ, payBankAccount));
                }
            }
        }
    }

    /**
     * 过滤交易类型
     *
     * @param bizObject
     * @param billDataDto
     * @throws Exception
     */
    private void filterTranstype(BizObject bizObject, BillDataDto billDataDto) throws Exception {

        FilterVO conditon = new FilterVO();
        if (billDataDto.getCondition() == null) {
            conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("billtype_id", ICmpConstant.QUERY_EQ, "FICA4"));
            if (bizObject != null) {
                String transferType = null;
                if ("bt".equals(bizObject.get("type")) || "BT".equals(bizObject.get("tradetype_code"))) {
                    transferType = "yhzz";
                } else if ("sc".equals(bizObject.get("type")) || "SC".equals(bizObject.get("tradetype_code"))) {
                    transferType = "jcxj";
                } else if ("ec".equals(bizObject.get("type")) || "EC".equals(bizObject.get("tradetype_code"))) {
                    transferType = "tqxj";
                } else if ("ct".equals(bizObject.get("type")) || "CT".equals(bizObject.get("tradetype_code"))) {
                    transferType = "xjhz";
                } else if ("tpt".equals(bizObject.get("type")) || "TPT".equals(bizObject.get("tradetype_code"))) {
                    transferType = "dsfzz";
                }
                if (transferType != null) {
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("extend_attrs_json", ICmpConstant.QUERY_LIKE, transferType));
                }
            }
            billDataDto.setCondition(conditon);
        } else {
            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("billtype_id", ICmpConstant.QUERY_EQ, "FICA4"));
            if (bizObject != null) {
                String transferType = null;
                if ("bt".equals(bizObject.get("type")) || "BT".equals(bizObject.get("tradetype_code"))) {
                    transferType = "yhzz";
                } else if ("sc".equals(bizObject.get("type")) || "SC".equals(bizObject.get("tradetype_code"))) {
                    transferType = "jcxj";
                } else if ("ec".equals(bizObject.get("type")) || "EC".equals(bizObject.get("tradetype_code"))) {
                    transferType = "tqxj";
                } else if ("ct".equals(bizObject.get("type")) || "CT".equals(bizObject.get("tradetype_code"))) {
                    transferType = "xjhz";
                } else if ("tpt".equals(bizObject.get("type")) || "TPT".equals(bizObject.get("tradetype_code"))) {
                    transferType = "dsfzz";
                }
                if (transferType != null) {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("extend_attrs_json", ICmpConstant.QUERY_LIKE, transferType));
                }
            }

        }
    }

    /**
     * 过滤结算方式
     *
     * @param bizObject
     * @param billDataDto
     * @throws Exception
     */
    private void filterSettlemethod(BizObject bizObject, BillDataDto billDataDto) throws Exception {
        List billDataDtoList = (List) billDataDto.getData();
        if (CollectionUtils.isEmpty(billDataDtoList)) {
            return;
        }

        TransferAccount transferAccount = (TransferAccount) billDataDtoList.get(0);
        String type = transferAccount.getType();
        BdTransType bdTransType = transTypeQueryService.findById(transferAccount.get("tradetype"));
        CtmJSONObject jsonObject = CtmJSONObject.parseObject(bdTransType.getExtendAttrsJson());
        String tradeTypeCode = (String) jsonObject.get("transferType_zz");
        FilterVO conditon = new FilterVO();
        if (billDataDto.getTreeCondition() == null) {
            billDataDto.setTreeCondition(conditon);
        }
        // 过滤付款结算方式
        filterPaySettleModelAttr(billDataDto, transferAccount, tradeTypeCode, type);
        // 过滤收款结算方式
        filterRecSettleModelAttr(billDataDto, tradeTypeCode, transferAccount);
        List<QueryOrderby> queryOrderlyList = new ArrayList<>();
        QueryOrderby orders = new QueryOrderby("order", ICmpConstant.ORDER_ASC);
        queryOrderlyList.add(orders);
        billDataDto.setQueryOrders(queryOrderlyList);


    }

    /**
     * 过滤收款结算方式
     *
     * @param billDataDto
     * @param tradeTypeCode
     * @param transferAccount
     */
    private void filterRecSettleModelAttr(BillDataDto billDataDto, String tradeTypeCode, TransferAccount transferAccount) {
        if (!"collectsettlemode_name".equals(billDataDto.getKey())) {
            return;
        }
        FilterVO filterVO =  billDataDto.getTreeCondition();
        if ("yhzz".equals(tradeTypeCode) || "jcxj".equalsIgnoreCase(tradeTypeCode)) { // sc:现金缴存-jcxj  bt:银行转账-yhzz
            filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 0));
        } else if ("dsfzz".equals(tradeTypeCode)) {//第三方转账
            if (null != transferAccount.getVirtualBank()) {
                if (transferAccount.getVirtualBank() == 0) {
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 0));
                } else {
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 10));
                }
            } else {
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 0));
            }
        } else if ("tqxj".equals(tradeTypeCode) || "xjhz".equals(tradeTypeCode)) {//现金提取 现金互转
            filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 1));
        }
        Set<String> digitalWalletTradeTypeCodes = new HashSet<>();
        digitalWalletTradeTypeCodes.add("sbqbcz");
        digitalWalletTradeTypeCodes.add("sbqbtx");
        digitalWalletTradeTypeCodes.add("sbqbhz");
        if (!digitalWalletTradeTypeCodes.contains(tradeTypeCode)) {
            return;
        }
        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 0));
    }

    /**
     * 过滤付款结算方式
     * @param billDataDto
     * @param transferAccount
     * @param tradeTypeCode
     * @param type
     */
    private void filterPaySettleModelAttr(BillDataDto billDataDto, TransferAccount transferAccount, String tradeTypeCode, String type) {
        if (!"settlemode_name".equals(billDataDto.getKey())) {
            return;
        }
        FilterVO filterVO =  billDataDto.getTreeCondition();
        if ("ct".equals(type) || "SC".equals(type) || "xjhz".equals(tradeTypeCode) || "jcxj".equalsIgnoreCase(tradeTypeCode)) {//ct:现金互转-xjhz     sc:现金缴存-jcxj
            filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 1));
        } else if ("tpt".equals(type) || "dsfzz".equals(tradeTypeCode)) {//第三方转账
            if (null != transferAccount.getVirtualBank()) {
                if (transferAccount.getVirtualBank() == 0) {
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 10));
                } else {
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 0));
                }
            } else {
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 10));
            }
        } else if ("ec".equals(type) || "tqxj".equals(tradeTypeCode)) {//现金提取
            filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_IN, new Integer[]{0, 8}));
        } else {
            filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 0));
        }
    }


}
