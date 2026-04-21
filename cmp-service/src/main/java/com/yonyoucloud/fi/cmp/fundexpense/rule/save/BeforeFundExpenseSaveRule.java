package com.yonyoucloud.fi.cmp.fundexpense.rule.save;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.enums.DfTypeEnum;
import com.yonyoucloud.fi.cmp.enums.ShareTypeEnum;
import com.yonyoucloud.fi.cmp.enums.SourceBillTypeEnum;
import com.yonyoucloud.fi.cmp.enums.SourceModelEnum;
import com.yonyoucloud.fi.cmp.fundexpense.Fundexpense;
import com.yonyoucloud.fi.cmp.fundexpense.Fundexpense_b;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.costcenter.CostCenterUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.biz.base.Objectlizer;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Component("beforeFundExpenseSaveRule")
public class BeforeFundExpenseSaveRule extends AbstractCommonRule {
    @Autowired
    private BaseRefRpcService baseRefRpcService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        String billnum = billContext.getBillnum();
        if (bills != null && bills.size() > 0) {
            // 获取前端传过来的值对象
            Fundexpense fundexpense = (Fundexpense) bills.get(0);
            // 前端传过来的子表
            List<Fundexpense_b> details = new ArrayList<>();
            if (fundexpense.detail() != null && CollectionUtils.isNotEmpty(fundexpense.detail())) {
                details = fundexpense.detail();
            }
            // 费用来源明细子表是否存在分摊开始日期、分摊结束日期、分摊周期、首次分摊日为空的数据
            if ("fundexpense".equals(billnum) || "Initialfundexpense".equals(billnum)) {
                if ("Update".equals(fundexpense.get("_status").toString())) {
                    handleUpdateDetail(fundexpense, details);
                }
                boolean isnull = false;
                // 费用来源明细子表数
                int detailsize = 0;
                // 主表费用金额
                BigDecimal expenseSum_fy = fundexpense.getExpenseSum_fy() != null ? fundexpense.getExpenseSum_fy() : BigDecimal.ZERO;
                // 子表费用金额合计
                BigDecimal expenseDetail = BigDecimal.ZERO;
                Map<String, List<String>> detailMap = new HashMap<>();
                List<Fundexpense_b> fundexpensedetails = fundexpense.detail();
                // 遍历子表，统计分摊日期等是否为空、累加子表费用金额、校验子表信息
                if (CollectionUtils.isNotEmpty(fundexpensedetails)) {
                    detailsize = fundexpensedetails.size();
                    String key = "";
                    for (Fundexpense_b fundexpensedetail : fundexpensedetails) {
                        if (fundexpensedetail.getShare_startdate() == null || fundexpensedetail.getShare_endate() == null
                                || fundexpensedetail.getShare_cycle() == null || fundexpensedetail.getFirst_sharedate() == null) {
                            isnull = true;
                        }
                        if (fundexpensedetail.getExpense_b_currency_expenseSum() != null) {
                            expenseDetail = expenseDetail.add(fundexpensedetail.getExpense_b_currency_expenseSum());
                        }
                        key = fundexpensedetail.getSrcbigtype() + "||" + fundexpensedetail.getSrcbilltype() + "||" + fundexpensedetail.getSrcbillno() + "||" + fundexpensedetail.getSrcexpenseplancode();
                        if (com.yonyou.cloud.utils.CollectionUtils.isEmpty(detailMap.get(key))) {
                            List<String> codeList = new ArrayList<>();
                            codeList.add(fundexpensedetail.getExpense_detail_code());
                            detailMap.put(key, codeList);
                        } else {
                            detailMap.get(key).add(fundexpensedetail.getExpense_detail_code());
                        }
                        // 子表校验
                        if (fundexpensedetail.getSrcbigtype() != null) {
                            // 来源模块
                            if (SourceModelEnum.getSourceModelByValue(fundexpensedetail.getSrcbigtype()) == null) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101439"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A05900053", "来源模块枚举值有误，保存失败！") /* "来源模块枚举值有误，保存失败！" */);
                            }
                        }
                        if (fundexpensedetail.getSrcbilltype() != null) {
                            // 来源单据类型
                            if (SourceBillTypeEnum.getSouceBillTypeByValue(fundexpensedetail.getSrcbilltype()) == null) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101440"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A05900057", "来源单据类型枚举值有误，保存失败！") /* "来源单据类型枚举值有误，保存失败！" */);
                            }
                        }
                        if (fundexpensedetail.getSrcbillno_name() != null) {
                            // 来源业务-导入根据名称、来源单据类型查询id
                            SourceBillTypeEnum sourceBillTypeEnum = SourceBillTypeEnum.getSouceBillTypeByValue(fundexpensedetail.getSrcbilltype());
                            String id = getIdByName(fundexpensedetail.getSrcbillno_name(), sourceBillTypeEnum.getFullName(), sourceBillTypeEnum.getField(), sourceBillTypeEnum.getDomain());
                            if (id == "") {
                                fundexpensedetail.setSrcbillno(null);
                                fundexpensedetail.setSrcbillno_name(null);
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101441"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A0590005D", "费用来源明细子表未查询到来源业务，保存失败！") /* "费用来源明细子表未查询到来源业务，保存失败！" */);
                            } else {
                                fundexpensedetail.setSrcbillno(id);
                            }
                        }
                        if (fundexpensedetail.getBusiness_currency() != null) {
                            // 业务币种已启用
                            checkBusinessCurrency(fundexpensedetail);
                        }
                        if (fundexpensedetail.getBusiness_currency_expenseSum() != null) {
                            // 费用金额（业务币种）>0
                            if (fundexpensedetail.getBusiness_currency_expenseSum().compareTo(BigDecimal.ZERO) <= 0) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101442"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A05900061", "子表费用金额（业务币种）需大于0，保存失败！") /* "子表费用金额（业务币种）需大于0，保存失败！" */);
                            }
                        }
                        if (fundexpensedetail.getBusiness_currency_tiaozhSum() != null) {
                            // 调整后金额（业务币种）>0
                            if (fundexpensedetail.getBusiness_currency_tiaozhSum().compareTo(BigDecimal.ZERO) <= 0) {
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101443"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A05900063", "子表调整后金额（业务币种）需大于0，保存失败！") /* "子表调整后金额（业务币种）需大于0，保存失败！" */);
                            }
                        }
                    }
                }
                if (expenseSum_fy.compareTo(expenseDetail) != 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101444"),MessageFormat.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A05900049", "费用金额{0}不等于子表费用金额（费用币种）合计值{1}，不允许保存！") /* "费用金额{0}不等于子表费用金额（费用币种）合计值{1}，不允许保存！" */, expenseSum_fy, expenseDetail));
                }
                if (detailsize < 1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101445"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A0590004A", "费用来源明细至少有一条明细！") /* "费用来源明细至少有一条明细！" */);
                }
                // 根据分摊类型校验费用来源明细
                if (fundexpense.getSharetype() != null) {
                    if (ShareTypeEnum.getShareTypeByValue(fundexpense.getSharetype()) == null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101446"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A0590004B", "分摊类型枚举值有误，保存失败！") /* "分摊类型枚举值有误，保存失败！" */);
                    } else if ((fundexpense.getSharetype().equals(ShareTypeEnum.TERM.getValue()) || fundexpense.getSharetype().equals(ShareTypeEnum.TERM_CONTRACT.getValue())) && isnull) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101447"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A0590004C", "分摊类型包含“期限”，费用来源明细所有行的“分摊开始日期、分摊结束日期、分摊周期、首次分摊日”均不能为空！") /* "分摊类型包含“期限”，费用来源明细所有行的“分摊开始日期、分摊结束日期、分摊周期、首次分摊日”均不能为空！" */);
                    } else if ((fundexpense.getSharetype().equals(ShareTypeEnum.TERM.getValue()) || fundexpense.getSharetype().equals(ShareTypeEnum.NO_SHARE.getValue())) && detailsize > 1) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101448"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A0590004E", "分摊类型“不分摊”或“期限”，子表行只能有一条明细！") /* "分摊类型“不分摊”或“期限”，子表行只能有一条明细！" */);
                    }
                }
                // 校验费用来源明细子表行重复
                for (String detailkey : detailMap.keySet()) {
                    List<String> detailcodelist = detailMap.get(detailkey);
                    if (detailcodelist.size() > 1) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101449"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A05900051", "费用来源明细存在“来源模块+来源单据类型+来源业务+来源费用计划”重复的数据，请修改！") /* "费用来源明细存在“来源模块+来源单据类型+来源业务+来源费用计划”重复的数据，请修改！" */);
                    }
                }
                // 根据参数补充利润中心、成本中心
                CostCenterUtils.setCostCenter(fundexpense, "dept", "accentity", "cost_center", "lirun_center");

                // 期初导入校验
                // 1.资金组织期初设置-现金管理期间启用
                if (fundexpense.getOrg() != null) {
                    List<Map<String, Object>> initSetting = QueryBaseDocUtils.queryOrgBpOrgConfVO(fundexpense.getOrg(), ISystemCodeConstant.ORG_MODULE_CM);
                    if (CollectionUtils.isEmpty(initSetting)) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101450"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A05900058", "当前资金组织的期初期间未启用！") /* "当前资金组织的期初期间未启用！" */);
                    }
                    fundexpense.put("accentity", fundexpense.getOrg());
                }
                // 2.费用方向枚举校验
                if (fundexpense.getExpenseDirect() != null && fundexpense.getExpenseDirect() != 1 && fundexpense.getExpenseDirect() != 2) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101451"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A0590005B", "费用方向枚举值有误，保存失败！") /* "费用方向枚举值有误，保存失败！" */);
                }
                // 3.费用项目已启用
                if (fundexpense.getExpenseitem() != null) {
                    checkExpenseitem(fundexpense);
                }
                // 4.费用日期校验费用日期小于当前资金组织现金管理模块的启用日期
                if (fundexpense.getExpensedate() != null) {
                    checkExpensedate(fundexpense);
                }
                // 5.费用币种已启用
                if (fundexpense.getExpensenatCurrency() != null) {
                    checkExpensenatCurrency(fundexpense);
                }
                // 6.本方账户正常
                if (fundexpense.getBankAccount() != null) {
                    checkEnterpriseBankAccount(fundexpense);
                }
                // 7.项目已启用
                if (fundexpense.getProject() != null) {
                    checkProject(fundexpense);
                }
                // 8.对方类型枚举校验、对方单位校验启用、对方银行账号补充id
                if (fundexpense.getDftype() != null) {
                    if (DfTypeEnum.getDfTypeByValue(fundexpense.getDftype()) == null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101452"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A05900066", "对方类型枚举值有误，保存失败！") /* "对方类型枚举值有误，保存失败！" */);
                    } else if (fundexpense.getDftype().equals(DfTypeEnum.CUSTOMER.getValue())) {
                        if (fundexpense.getDfenterprise_customer_name() != null) {
                            handleFundExpenseCustomer(fundexpense);
                        }
                        if (fundexpense.getDfcustomerbankaccount_account() != null) {
                            handleDfCustomerBankAccount(fundexpense);
                        }
                    } else if (fundexpense.getDftype().equals(DfTypeEnum.SUPPLIER.getValue())) {
                        if (fundexpense.getDfenterprise_customer_name() != null) {
                            handleFundExpenseSupplier(fundexpense);
                            fundexpense.setDfenterprise_customer_name(null);
                        }
                        if (fundexpense.getDfcustomerbankaccount_account() != null) {
                            handleDfSupplierBankAccount(fundexpense);
                            fundexpense.setDfcustomerbankaccount_account(null);
                        }
                    } else if (fundexpense.getDftype().equals(DfTypeEnum.FUNBUSOBJ.getValue())) {
                        if (fundexpense.getDfenterprise_customer_name() != null) {
                            handleFundExpenseFundBusinObj(fundexpense);
                            fundexpense.setDfenterprise_customer_name(null);
                        }
                        if (fundexpense.getDfcustomerbankaccount_account() != null) {
                            handleFunbusobjBankAccount(fundexpense);
                            fundexpense.setDfcustomerbankaccount_account(null);
                        }
                    }
                }
            }
        }
        return new RuleExecuteResult();
    }

    /**
     * 更新态补充子表数据
     *
     * @param fundexpense
     * @param details
     * @throws Exception
     */
    private void handleUpdateDetail(Fundexpense fundexpense, List<Fundexpense_b> details) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("*");
        QueryConditionGroup condition = new QueryConditionGroup();
        condition.addCondition(QueryCondition.name("mainid").eq(fundexpense.getId()));
        schema.addCondition(condition);
        List<Map<String, Object>> fundexpenseFromDBList = MetaDaoHelper.query(Fundexpense_b.ENTITY_NAME, schema);
        if (CollectionUtils.isNotEmpty(fundexpenseFromDBList)) {
            List<Fundexpense_b> addDetails = new ArrayList<>();
            // 前端传过来的子表为空，将库里的子表直接赋值
            if (CollectionUtils.isEmpty(details)) {
                for (Map<String, Object> fundexpenseFromDB : fundexpenseFromDBList) {
                    addDetails.add(Objectlizer.convert(fundexpenseFromDB, Fundexpense_b.ENTITY_NAME));
                }
            } else if (fundexpenseFromDBList != null) {
                for (Map<String, Object> detailFromDB : fundexpenseFromDBList) {
                    boolean isSame = false;
                    for (Fundexpense_b detail : details) {
                        if (detailFromDB.get("id").equals(detail.getId())) {
                            isSame = true;
                            break;
                        }
                    }
                    if (!isSame) {
                        addDetails.add(Objectlizer.convert(detailFromDB, Fundexpense_b.ENTITY_NAME));
                    }
                }
            }
            details.addAll(addDetails);
            fundexpense.setDetail(details);
        }
    }

    /**
     * 处理对方银行账号-客户
     *
     * @param fundexpense
     * @throws Exception
     */
    private void handleDfCustomerBankAccount(Fundexpense fundexpense) throws Exception {
        List<Map<String, Object>> merchantList = getBankAccountByName("bankAccount", fundexpense.getDfcustomerbankaccount_account(), ICsplConstant.AGENTFINANCIAL_FULL_NAME, ISchemaConstant.MDD_SCHEMA_PRODUCTCENTER);
        if (CollectionUtils.isNotEmpty(merchantList)) {
            if (merchantList.get(0).get("id") != null) {
                fundexpense.setDfcustomerbankaccount(Long.valueOf(merchantList.get(0).get("id").toString()));
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101453"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A05900050", "未查询到客户银行账号，保存失败！") /* "未查询到客户银行账号，保存失败！" */);
        }
    }

    /**
     * 处理对方银行账号-供应商
     *
     * @param fundexpense
     * @throws Exception
     */
    private void handleDfSupplierBankAccount(Fundexpense fundexpense) throws Exception {
        List<Map<String, Object>> supplierList = getBankAccountByName("account", fundexpense.getDfcustomerbankaccount_account(), ICsplConstant.VENDORBANK_FULL_NAME, ISchemaConstant.MDD_SCHEMA_YSSUPPLIER);
        if (CollectionUtils.isNotEmpty(supplierList)) {
            if (supplierList.get(0).get("id") != null) {
                fundexpense.setDfsupplierbankaccount(Long.valueOf(supplierList.get(0).get("id").toString()));
            }
            fundexpense.setDfsupplierbankaccount_account(fundexpense.getDfcustomerbankaccount_account());
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101454"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A0590005C", "未查询到供应商银行账号，保存失败！") /* "未查询到供应商银行账号，保存失败！" */);
        }
    }

    /**
     * 处理对方银行账号-资金伙伴
     *
     * @param fundexpense
     * @throws Exception
     */
    private void handleFunbusobjBankAccount(Fundexpense fundexpense) throws Exception {
        List<Map<String, Object>> fundBusinObjList = getBankAccountByName("bankaccount", fundexpense.getDfcustomerbankaccount_account(), ICsplConstant.FUNDBUSINOBJARCHIVESITEM_FULL_NAME, IDomainConstant.MDD_DOMAIN_TMSP);
        if (CollectionUtils.isNotEmpty(fundBusinObjList)) {
            if (fundBusinObjList.get(0).get("id") != null) {
                fundexpense.setDffunbusobjbankaccount(String.valueOf(fundBusinObjList.get(0).get("id")));
            }
            fundexpense.setDffunbusobjbankaccount_account(fundexpense.getDfcustomerbankaccount_account());
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101455"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A05900062", "未查询到资金伙伴银行账号，保存失败！") /* "未查询到资金伙伴银行账号，保存失败！" */);
        }
    }

    /**
     * 根据银行账号查询id
     *
     * @param accountField
     * @param account
     * @param fullname
     * @param domain
     * @return
     * @throws Exception
     */
    private List<Map<String, Object>> getBankAccountByName(String accountField, String account, String fullname, String domain) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("id");
        QueryConditionGroup condition = new QueryConditionGroup();
        condition.addCondition(QueryCondition.name(accountField).eq(account));
        schema.addCondition(condition);
        return MetaDaoHelper.query(fullname, schema, domain);
    }

    /**
     * 校验子表业务币种已启用
     *
     * @param fundexpensedetail
     * @throws Exception
     */
    private void checkBusinessCurrency(Fundexpense_b fundexpensedetail) throws Exception {
        List<Map<String, Object>> currencyList = QueryBaseDocUtils.queryCurrencyById(fundexpensedetail.getBusiness_currency());
        if (CollectionUtils.isNotEmpty(currencyList)) {
            Object currencyFlag = currencyList.get(0).get("enable");
            // 1-启用，2-未启用
            if (currencyFlag != null && "2".equals(currencyFlag.toString())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101456"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A05900054", "子表业务币种未启用，保存失败！") /* "子表业务币种未启用，保存失败！" */);
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101457"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A05900055", "未查询到对应的业务币种，保存失败！") /* "未查询到对应的业务币种，保存失败！" */);
        }
    }

    /**
     * 校验费用项目已启用
     *
     * @param fundexpense
     */
    private void checkExpenseitem(Fundexpense fundexpense) throws Exception {
        List<Map<String, Object>> expenseItemList = QueryBaseDocUtils.queryExpenseItemById(fundexpense.getExpenseitem());
        if (CollectionUtils.isNotEmpty(expenseItemList)) {
            // true-启用，false-未启用
            Object expenseItemFlag = expenseItemList.get(0).get("enabled");
            if (expenseItemFlag != null && !(boolean) expenseItemFlag) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101458"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A0590005E", "费用项目未启用，保存失败！") /* "费用项目未启用，保存失败！" */);
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101459"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A0590005F", "未查询到对应的费用项目，保存失败！") /* "未查询到对应的费用项目，保存失败！" */);
        }
    }

    /**
     * 校验费用日期大于资金组织-现金管理-期初日期
     *
     * @param fundexpense
     * @throws Exception
     */
    private void checkExpensedate(Fundexpense fundexpense) throws Exception {
        Date initDate = QueryBaseDocUtils.queryOrgPeriodBeginDate(fundexpense.getOrg(), ISystemCodeConstant.ORG_MODULE_CM);
        if (DateUtils.dateCompare(initDate, fundexpense.get("expensedate")) > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101460"),MessageFormat.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A05900064", "费用日期不能小于该模块启用日期:{0}！") /* "费用日期不能小于该模块启用日期:{0}！" */, sdf.format(initDate)));
        }
    }

    /**
     * 校验费用币种已启用
     *
     * @param fundexpense
     */
    private void checkExpensenatCurrency(Fundexpense fundexpense) throws Exception {
        List<Map<String, Object>> currencyList = QueryBaseDocUtils.queryCurrencyById(fundexpense.getExpensenatCurrency());
        if (CollectionUtils.isNotEmpty(currencyList)) {
            Object currencyFlag = currencyList.get(0).get("enable");
            //1-启用，2-未启用
            if (currencyFlag != null && "2".equals(currencyFlag.toString())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101461"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A0590004D", "费用币种未启用，保存失败！") /* "费用币种未启用，保存失败！" */);
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101462"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A0590004F", "未查询到对应的费用币种，保存失败！") /* "未查询到对应的费用币种，保存失败！" */);
        }
    }

    /**
     * 校验项目已启用
     *
     * @param fundexpense
     * @throws Exception
     */
    private void checkProject(Fundexpense fundexpense) throws Exception {
        List<Map<String, Object>> projectList = QueryBaseDocUtils.queryProjectById(fundexpense.getProject());
        if (CollectionUtils.isNotEmpty(projectList)) {
            Object projectFlag = projectList.get(0).get("enable");
            // 1-启用，2-未启用
            if (projectFlag != null && "2".equals(projectFlag.toString())) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101463"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A05900056", "项目未启用，保存失败！") /* "项目未启用，保存失败！" */);
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101464"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A05900059", "未查询到对应的项目，保存失败！") /* "未查询到对应的项目，保存失败！" */);
        }
    }

    /**
     * 处理对方单位-客户
     *
     * @param fundexpense
     * @throws Exception
     */
    private void handleFundExpenseCustomer(Fundexpense fundexpense) throws Exception {
        List<Map<String, Object>> merchantList = queryMerchantByNameAndOrg(fundexpense.getDfenterprise_customer_name(), fundexpense.getOrg());
        if (CollectionUtils.isNotEmpty(merchantList)) {
            if (merchantList.get(0).get("id") != null) {
                fundexpense.setDfenterprise_customer(String.valueOf(merchantList.get(0).get("id")));
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101465"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A05900060", "未查询到该启用客户或客户与资金组织不匹配，保存失败！") /* "未查询到该启用客户或客户与资金组织不匹配，保存失败！" */);
        }
    }

    /**
     * 根据客户名称与资金组织查询启用状态客户信息
     *
     * @param customer_name
     * @param org
     * @return
     * @throws Exception
     */
    private List<Map<String, Object>> queryMerchantByNameAndOrg(String customer_name, String org) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("id, name");
        QueryConditionGroup condition = new QueryConditionGroup();
        condition.addCondition(QueryCondition.name("name").eq(customer_name));
        condition.addCondition(QueryCondition.name("merchantAppliedDetail.merchantApplyRangeExtId.orgId").eq(org));
        condition.addCondition(QueryCondition.name("merchantAppliedDetail.stopstatus").eq(false));
        schema.addCondition(condition);
        return MetaDaoHelper.query(ICsplConstant.MERCHANT_FULL_NAME, schema, ISchemaConstant.MDD_SCHEMA_PRODUCTCENTER);
    }

    /**
     * 处理对方单位-供应商
     *
     * @param fundexpense
     * @throws Exception
     */
    private void handleFundExpenseSupplier(Fundexpense fundexpense) throws Exception {
        List<Map<String, Object>> supplierList = querySupplierByNameAndOrg(fundexpense.getDfenterprise_customer_name());
        if (CollectionUtils.isNotEmpty(supplierList)) {
            if (supplierList.get(0).get("id") != null) {
                fundexpense.setDfenterprise_supplier(Long.valueOf(supplierList.get(0).get("id").toString()));
            }
            if (supplierList.get(0).get("name") != null) {
                fundexpense.setDfenterprise_supplier_name(String.valueOf(supplierList.get(0).get("name")));
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101466"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A05900052", "未查询到该启用供应商，保存失败！") /* "未查询到该启用供应商，保存失败！" */);
        }
    }

    /**
     * 根据供应商名称与资金组织查询启用状态供应商信息
     *
     * @param supplier_name
     * @return
     * @throws Exception
     */
    private List<Map<String, Object>> querySupplierByNameAndOrg(String supplier_name) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("id, name");
        QueryConditionGroup condition = new QueryConditionGroup();
        condition.addCondition(QueryCondition.name("name").eq(supplier_name));
        condition.addCondition(QueryCondition.name("vendorextends.stopstatus").eq(false));
        schema.addCondition(condition);
        return MetaDaoHelper.query(ICsplConstant.VENDOR_FULL_NAME, schema, ISchemaConstant.MDD_SCHEMA_YSSUPPLIER);
    }

    /**
     * 处理对方单位-资金伙伴
     *
     * @param fundexpense
     * @throws Exception
     */
    private void handleFundExpenseFundBusinObj(Fundexpense fundexpense) throws Exception {
        List<Map<String, Object>> fundBusinObjList = getFundBusinObjByNameAndOrg(fundexpense.getDfenterprise_customer_name(), fundexpense.getOrg());
        if (CollectionUtils.isNotEmpty(fundBusinObjList)) {
            if (fundBusinObjList.get(0).get("id") != null) {
                fundexpense.setDfenterprise_funbusobj(String.valueOf(fundBusinObjList.get(0).get("id")));
            }
            if (fundBusinObjList.get(0).get("fundbusinobjtypename") != null) {
                fundexpense.setDfenterprise_funbusobj_name(String.valueOf(fundBusinObjList.get(0).get("fundbusinobjtypename")));
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101467"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A05900065", "未查询到该启用资金伙伴或资金伙伴使用组织与资金组织不匹配，保存失败！") /* "未查询到该启用资金伙伴或资金伙伴使用组织与资金组织不匹配，保存失败！" */);
        }
    }

    /**
     * 根据资金伙伴名称与资金组织查询启用状态资金伙伴信息（资金业务对象）
     *
     * @param fundbusinobj_name
     * @param org
     * @return
     * @throws Exception
     */
    private static List<Map<String, Object>> getFundBusinObjByNameAndOrg(String fundbusinobj_name, String org) throws Exception {
        QuerySchema schema = QuerySchema.create().addSelect("id, fundbusinobjtypename");
        QueryConditionGroup condition = new QueryConditionGroup();
        condition.addCondition(QueryCondition.name("fundbusinobjtypename").eq(fundbusinobj_name));
        condition.addCondition(QueryCondition.name("accentity").eq(org));
        condition.addCondition(QueryCondition.name("enabled").eq(1));
        schema.addCondition(condition);
        return MetaDaoHelper.query(ICsplConstant.FUNDBUSINOBJARCHIVES_FULL_NAME, schema, IDomainConstant.MDD_DOMAIN_TMSP);
    }

    /**
     * 本方账户校验
     *
     * @param fundexpense
     * @throws Exception
     */
    private void checkEnterpriseBankAccount(Fundexpense fundexpense) throws Exception {
        String enterpriseBankAccount = fundexpense.getBankAccount();
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setId(enterpriseBankAccount);
        enterpriseParams.setOrgid(fundexpense.getOrg());
        enterpriseParams.setAcctstatus(0);
        enterpriseParams.setCurrencyIDList(Collections.singletonList(fundexpense.getExpensenatCurrency()));
        List<EnterpriseBankAcctVO> bankAccounts = baseRefRpcService.queryEnterpriseBankAccountByCondition(enterpriseParams);
        if (bankAccounts.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101468"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D07E86A0590005A", "本方账户资金组织使用权需要等于资金组织、账户状态为正常且币种等于费用币种，保存失败！") /* "本方账户资金组织使用权需要等于资金组织、账户状态为正常且币种等于费用币种，保存失败！" */);
        }
    }

    /**
     * 来源业务根据编号补充id
     *
     * @param srcbillno_name
     * @param fullName
     * @param field
     * @param domain
     * @return
     * @throws Exception
     */
    private String getIdByName(String srcbillno_name, String fullName, String field, String domain) throws Exception {
        String id = "";
        QuerySchema schema = QuerySchema.create().addSelect("id");
        schema.appendQueryCondition(QueryCondition.name(field).eq(srcbillno_name));
        List<Map<String, Object>> result = MetaDaoHelper.query(fullName, schema, domain);
        if (CollectionUtils.isNotEmpty(result)) {
            Map<String, Object> res = result.get(0);
            if (res.get("id") != null) {
                id = String.valueOf(res.get("id"));
            }
        }
        return id;
    }
}
